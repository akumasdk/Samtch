(function() {
    'use strict';
    const ourTwitchAdSolutionsVersion = 25; // Bumped version for Samtch hydration refactor

    // Self-cleanup: Clear existing intervals/timeouts from previous runs if any
    if (window.samtch_vaft_monitor_timer) clearTimeout(window.samtch_vaft_monitor_timer);

    if (typeof window.twitchAdSolutionsVersion !== 'undefined' && window.twitchAdSolutionsVersion > ourTwitchAdSolutionsVersion) {
        console.log("skipping vaft as there's a newer script active. ourVersion:" + ourTwitchAdSolutionsVersion + " activeVersion:" + window.twitchAdSolutionsVersion);
        return;
    }
    window.twitchAdSolutionsVersion = ourTwitchAdSolutionsVersion;

    console.log('[Samtch] vaft.js (AdBlock) starting fresh session...');

    function declareOptions(scope) {
        scope.AdSignifier = 'stitched';
        scope.ClientID = 'kimne78kx3ncx6brgo4mv6wki5h1ko';
        scope.BackupPlayerTypes = ['embed', 'popout', 'autoplay'];
        scope.FallbackPlayerType = 'embed';
        scope.ForceAccessTokenPlayerType = 'popout';
        scope.SkipPlayerReloadOnHevc = false;
        scope.AlwaysReloadPlayerOnAd = false;
        scope.ReloadPlayerAfterAd = true;
        scope.PlayerReloadMinimalRequestsTime = 1500;
        scope.PlayerReloadMinimalRequestsPlayerIndex = 2;
        scope.HasTriggeredPlayerReload = false;
        scope.StreamInfos = [];
        scope.StreamInfosByUrl = [];
        scope.GQLDeviceID = null;
        scope.ClientVersion = null;
        scope.ClientSession = null;
        scope.ClientIntegrityHeader = null;
        scope.AuthorizationHeader = undefined;
        scope.SimulatedAdsDepth = 0;
        scope.PlayerBufferingFix = true;
        scope.PlayerBufferingDelay = 600;
        scope.PlayerBufferingSameStateCount = 3;
        scope.PlayerBufferingDangerZone = 1;
        scope.PlayerBufferingDoPlayerReload = false;
        scope.PlayerBufferingMinRepeatDelay = 8000;
        scope.PlayerBufferingPrerollCheckEnabled = false;
        scope.PlayerBufferingPrerollCheckOffset = 5;
        scope.V2API = false;
        scope.IsAdStrippingEnabled = true;
        scope.AdSegmentCache = new Map();
        scope.AllSegmentsAreAdSegments = false;
    }

    let isActivelyStrippingAds = false;
    let localStorageHookFailed = false;
    const twitchWorkers = [];
    const workerStringConflicts = ['twitch', 'isVariantA'];
    const workerStringAllow = [];
    const workerStringReinsert = ['isVariantA', 'besuper/', '${patch_url}'];

    function getCleanWorker(worker) {
        let root = null;
        let parent = null;
        let proto = worker;
        while (proto) {
            const workerString = proto.toString();
            if (workerStringConflicts.some((x) => workerString.includes(x)) && !workerStringAllow.some((x) => workerString.includes(x))) {
                if (parent !== null) {
                    Object.setPrototypeOf(parent, Object.getPrototypeOf(proto));
                }
            } else {
                if (root === null) {
                    root = proto;
                }
                parent = proto;
            }
            proto = Object.getPrototypeOf(proto);
        }
        return root;
    }

    function getWorkersForReinsert(worker) {
        const result = [];
        let proto = worker;
        while (proto) {
            const workerString = proto.toString();
            if (workerStringReinsert.some((x) => workerString.includes(x))) {
                result.push(proto);
            }
            proto = Object.getPrototypeOf(proto);
        }
        return result;
    }

    function reinsertWorkers(worker, reinsert) {
        let parent = worker;
        for (let i = 0; i < reinsert.length; i++) {
            Object.setPrototypeOf(reinsert[i], parent);
            parent = reinsert[i];
        }
        return parent;
    }

    function isValidWorker(worker) {
        const workerString = worker.toString();
        return !workerStringConflicts.some((x) => workerString.includes(x))
            || workerStringAllow.some((x) => workerString.includes(x))
            || workerStringReinsert.some((x) => workerString.includes(x));
    }

    function hookWindowWorker() {
        if (window.Worker && window.Worker.samtch_hooked) return;
        const reinsert = getWorkersForReinsert(window.Worker);
        const baseWorker = getCleanWorker(window.Worker);
        if (!baseWorker) return;

        const newWorker = class Worker extends baseWorker {
            constructor(twitchBlobUrl, options) {
                let isTwitchWorker = false;
                try {
                    isTwitchWorker = new URL(twitchBlobUrl).origin.endsWith('.twitch.tv');
                } catch {}
                if (!isTwitchWorker) {
                    super(twitchBlobUrl, options);
                    return;
                }
                const newBlobStr = `
                    const pendingFetchRequests = new Map();
                    ${stripAdSegments.toString()}
                    ${getStreamUrlForResolution.toString()}
                    ${processM3U8.toString()}
                    ${hookWorkerFetch.toString()}
                    ${declareOptions.toString()}
                    ${getAccessToken.toString()}
                    ${gqlRequest.toString()}
                    ${parseAttributes.toString()}
                    ${getWasmWorkerJs.toString()}
                    ${getServerTimeFromM3u8.toString()}
                    ${replaceServerTimeInM3u8.toString()}
                    const workerString = getWasmWorkerJs('${twitchBlobUrl.replaceAll("'", "%27")}');
                    declareOptions(self);
                    GQLDeviceID = ${GQLDeviceID ? "'" + GQLDeviceID + "'" : null};
                    AuthorizationHeader = ${AuthorizationHeader ? "'" + AuthorizationHeader + "'" : undefined};
                    ClientIntegrityHeader = ${ClientIntegrityHeader ? "'" + ClientIntegrityHeader + "'" : null};
                    ClientVersion = ${ClientVersion ? "'" + ClientVersion + "'" : null};
                    ClientSession = ${ClientSession ? "'" + ClientSession + "'" : null};
                    self.addEventListener('message', function(e) {
                        if (e.data.key == 'UpdateClientVersion') {
                            ClientVersion = e.data.value;
                        } else if (e.data.key == 'UpdateClientSession') {
                            ClientSession = e.data.value;
                        } else if (e.data.key == 'UpdateClientId') {
                            ClientID = e.data.value;
                        } else if (e.data.key == 'UpdateDeviceId') {
                            GQLDeviceID = e.data.value;
                        } else if (e.data.key == 'UpdateClientIntegrityHeader') {
                            ClientIntegrityHeader = e.data.value;
                        } else if (e.data.key == 'UpdateAuthorizationHeader') {
                            AuthorizationHeader = e.data.value;
                        } else if (e.data.key == 'FetchResponse') {
                            const responseData = e.data.value;
                            if (pendingFetchRequests.has(responseData.id)) {
                                const { resolve, reject } = pendingFetchRequests.get(responseData.id);
                                pendingFetchRequests.delete(responseData.id);
                                if (responseData.error) {
                                    reject(new Error(responseData.error));
                                } else {
                                    const response = new Response(responseData.body, {
                                        status: responseData.status,
                                        statusText: responseData.statusText,
                                        headers: responseData.headers
                                    });
                                    resolve(response);
                                }
                            }
                        } else if (e.data.key == 'TriggeredPlayerReload') {
                            HasTriggeredPlayerReload = true;
                        } else if (e.data.key == 'SimulateAds') {
                            SimulatedAdsDepth = e.data.value;
                        } else if (e.data.key == 'AllSegmentsAreAdSegments') {
                            AllSegmentsAreAdSegments = !AllSegmentsAreAdSegments;
                        }
                    });
                    hookWorkerFetch();
                    eval(workerString);
                `;
                super(URL.createObjectURL(new Blob([newBlobStr])), options);
                twitchWorkers.push(this);
                this.addEventListener('message', (e) => {
                    if (e.data.key == 'UpdateAdBlockBanner') {
                        updateAdblockBanner(e.data);
                    } else if (e.data.key == 'PauseResumePlayer') {
                        doTwitchPlayerTask(true, false);
                    } else if (e.data.key == 'ReloadPlayer') {
                        doTwitchPlayerTask(false, true);
                    }
                });
                this.addEventListener('message', async event => {
                    if (event.data.key == 'FetchRequest') {
                        const fetchRequest = event.data.value;
                        const responseData = await handleWorkerFetchRequest(fetchRequest);
                        this.postMessage({
                            key: 'FetchResponse',
                            value: responseData
                        });
                    }
                });
            }
        };
        newWorker.samtch_hooked = true;
        let workerInstance = reinsertWorkers(newWorker, reinsert);
        Object.defineProperty(window, 'Worker', {
            get: function() { return workerInstance; },
            set: function(value) {
                if (isValidWorker(value)) {
                    workerInstance = value;
                } else {
                    console.log('[Samtch] Attempt to set non-Samtch worker denied');
                }
            }
        });
    }

    function getWasmWorkerJs(twitchBlobUrl) {
        const req = new XMLHttpRequest();
        req.open('GET', twitchBlobUrl, false);
        req.overrideMimeType("text/javascript");
        req.send();
        return req.responseText;
    }

    function hookWorkerFetch() {
        const realFetch = fetch;
        fetch = async function(url, options) {
            if (typeof url === 'string') {
                if (AdSegmentCache.has(url)) {
                    return new Promise(function(resolve, reject) {
                        realFetch('data:video/mp4;base64,AAAAKGZ0eXBtcDQyAAAAAWlzb21tcDQyZGFzaGF2YzFpc282aGxzZgAABEltb292AAAAbG12aGQAAAAAAAAAAAAAAAAAAYagAAAAAAABAAABAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADAAABqHRyYWsAAABcdGtoZAAAAAMAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAAAAAEAAAAAAQAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAURtZGlhAAAAIG1kaGQAAAAAAAAAAAAAAAAAALuAAAAAAFXEAAAAAAAtaGRscgAAAAAAAAAAc291bgAAAAAAAAAAAAAAAFNvdW5kSGFuZGxlcgAAAADvbWluZgAAABBzbWhkAAAAAAAAAAAAAAAkZGluZgAAABxkcmVmAAAAAAAAAAEAAAAMdXJsIAAAAAEAAACzc3RibAAAAGdzdHNkAAAAAAAAAAEAAABXbXA0YQAAAAAAAAABAAAAAAAAAAAAAgAQAAAAALuAAAAAAAAzZXNkcwAAAAADgICAIgABAASAgIAUQBUAAAAAAAAAAAAAAAWAgIACEZAGgICAAQIAAAAQc3R0cwAAAAAAAAAAAAAAEHN0c2MAAAAAAAAAAAAAABRzdHN6AAAAAAAAAAAAAAAAAAAAEHN0Y28AAAAAAAAAAAAAAeV0cmFrAAAAXHRraGQAAAADAAAAAAAAAAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAABAAAAAAoAAAAFoAAAAAAGBbWRpYQAAACBtZGhkAAAAAAAAAAAAAAAAAA9CQAAAAABVxAAAAAAALWhkbHIAAAAAAAAAAHZpZGUAAAAAAAAAAAAAAABWaWRlb0hhbmRsZXIAAAABLG1pbmYAAAAUdm1oZAAAAAEAAAAAAAAAAAAAACRkaW5mAAAAHGRyZWYAAAAAAAAAAQAAAAx1cmwgAAAAAQAAAOxzdGJsAAAAoHN0c2QAAAAAAAAAAQAAAJBhdmMxAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAoABaABIAAAASAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAGP//AAAAOmF2Y0MBTUAe/+EAI2dNQB6WUoFAX/LgLUBAQFAAAD6AAA6mDgAAHoQAA9CW7y4KAQAEaOuPIAAAABBzdHRzAAAAAAAAAAAAAAAQc3RzYwAAAAAAAAAAAAAAFHN0c3oAAAAAAAAAAAAAAAAAAAAQc3RjbwAAAAAAAAAAAAAASG12ZXgAAAAgdHJleAAAAAAAAAABAAAAAQAAAC4AAAAAAoAAAAAAACB0cmV4AAAAAAAAAAIAAAABAACCNQAAAAACQAAA', options)
                        .then(resolve).catch(reject);
                    });
                }
                url = url.trimEnd();
                if (url.endsWith('m3u8')) {
                    return new Promise(function(resolve, reject) {
                        realFetch(url, options).then(async (response) => {
                            if (response.status === 200) {
                                resolve(new Response(await processM3U8(url, await response.text(), realFetch)));
                            } else {
                                resolve(response);
                            }
                        }).catch(reject);
                    });
                } else if (url.includes('/channel/hls/') && !url.includes('picture-by-picture')) {
                    V2API = url.includes('/api/v2/');
                    const channelName = (new URL(url)).pathname.match(/([^\/]+)(?=\.\w+$)/)[0];
                    if (ForceAccessTokenPlayerType) {
                        const tempUrl = new URL(url);
                        tempUrl.searchParams.delete('parent_domains');
                        url = tempUrl.toString();
                    }
                    return new Promise(function(resolve, reject) {
                        realFetch(url, options).then(async (response) => {
                            if (response.status == 200) {
                                const encodingsM3u8 = await response.text();
                                const serverTime = getServerTimeFromM3u8(encodingsM3u8);
                                let streamInfo = StreamInfos[channelName];
                                if (streamInfo != null && streamInfo.EncodingsM3U8 != null && (await realFetch(streamInfo.EncodingsM3U8.match(/^https:.*\.m3u8$/m)[0])).status !== 200) {
                                    streamInfo = null;
                                }
                                if (streamInfo == null || streamInfo.EncodingsM3U8 == null) {
                                    StreamInfos[channelName] = streamInfo = {
                                        ChannelName: channelName,
                                        IsShowingAd: false,
                                        LastPlayerReload: 0,
                                        EncodingsM3U8: encodingsM3u8,
                                        ModifiedM3U8: null,
                                        IsUsingModifiedM3U8: false,
                                        UsherParams: (new URL(url)).search,
                                        RequestedAds: new Set(),
                                        Urls: [],
                                        ResolutionList: [],
                                        BackupEncodingsM3U8Cache: [],
                                        ActiveBackupPlayerType: null,
                                        IsMidroll: false,
                                        IsStrippingAdSegments: false,
                                        NumStrippedAdSegments: 0
                                    };
                                    const lines = encodingsM3u8.replaceAll('\r', '').split('\n');
                                    for (let i = 0; i < lines.length - 1; i++) {
                                        if (lines[i].startsWith('#EXT-X-STREAM-INF') && lines[i + 1].includes('.m3u8')) {
                                            const attributes = parseAttributes(lines[i]);
                                            const resolution = attributes['RESOLUTION'];
                                            if (resolution) {
                                                const resolutionInfo = {
                                                    Resolution: resolution,
                                                    FrameRate: attributes['FRAME-RATE'],
                                                    Codecs: attributes['CODECS'],
                                                    Url: lines[i + 1]
                                                };
                                                streamInfo.Urls[lines[i + 1]] = resolutionInfo;
                                                streamInfo.ResolutionList.push(resolutionInfo);
                                            }
                                            StreamInfosByUrl[lines[i + 1]] = streamInfo;
                                        }
                                    }
                                    const nonHevcResolutionList = streamInfo.ResolutionList.filter((element) => element.Codecs.startsWith('avc') || element.Codecs.startsWith('av0'));
                                    if (AlwaysReloadPlayerOnAd || (nonHevcResolutionList.length > 0 && streamInfo.ResolutionList.some((element) => element.Codecs.startsWith('hev') || element.Codecs.startsWith('hvc')) && !SkipPlayerReloadOnHevc)) {
                                        if (nonHevcResolutionList.length > 0) {
                                            for (let i = 0; i < lines.length - 1; i++) {
                                                if (lines[i].startsWith('#EXT-X-STREAM-INF')) {
                                                    const resSettings = parseAttributes(lines[i].substring(lines[i].indexOf(':') + 1));
                                                    const codecsKey = 'CODECS';
                                                    if (resSettings[codecsKey].startsWith('hev') || resSettings[codecsKey].startsWith('hvc')) {
                                                        const oldResolution = resSettings['RESOLUTION'];
                                                        const [targetWidth, targetHeight] = oldResolution.split('x').map(Number);
                                                        const newResolutionInfo = nonHevcResolutionList.sort((a, b) => {
                                                            const [streamWidthA, streamHeightA] = a.Resolution.split('x').map(Number);
                                                            const [streamWidthB, streamHeightB] = b.Resolution.split('x').map(Number);
                                                            return Math.abs((streamWidthA * streamHeightA) - (targetWidth * targetHeight)) - Math.abs((streamWidthB * streamHeightB) - (targetWidth * targetHeight));
                                                        })[0];
                                                        lines[i] = lines[i].replace(/CODECS="[^"]+"/, `CODECS="${newResolutionInfo.Codecs}"`);
                                                        lines[i + 1] = newResolutionInfo.Url + ' '.repeat(i + 1);
                                                    }
                                                }
                                            }
                                        }
                                        if (nonHevcResolutionList.length > 0 || AlwaysReloadPlayerOnAd) {
                                            streamInfo.ModifiedM3U8 = lines.join('\n');
                                        }
                                    }
                                }
                                streamInfo.LastPlayerReload = Date.now();
                                resolve(new Response(replaceServerTimeInM3u8(streamInfo.IsUsingModifiedM3U8 ? streamInfo.ModifiedM3U8 : streamInfo.EncodingsM3U8, serverTime)));
                            } else {
                                resolve(response);
                            }
                        }).catch(reject);
                    });
                }
            }
            return realFetch.apply(this, arguments);
        };
    }

    function getServerTimeFromM3u8(encodingsM3u8) {
        if (V2API) {
            const matches = encodingsM3u8.match(/#EXT-X-SESSION-DATA:DATA-ID="SERVER-TIME",VALUE="([^"]+)"/);
            return matches ? (matches[1] || null) : null;
        }
        const matches = encodingsM3u8.match('SERVER-TIME="([0-9.]+)"');
        return matches ? (matches[1] || null) : null;
    }

    function replaceServerTimeInM3u8(encodingsM3u8, newServerTime) {
        if (V2API) {
            return newServerTime ? encodingsM3u8.replace(/(#EXT-X-SESSION-DATA:DATA-ID="SERVER-TIME",VALUE=")[^"]+(")/, `$1${newServerTime}$2`) : encodingsM3u8;
        }
        return newServerTime ? encodingsM3u8.replace(new RegExp('(SERVER-TIME=")[0-9.]+"'), `SERVER-TIME="${newServerTime}"`) : encodingsM3u8;
    }

    function stripAdSegments(textStr, stripAllSegments, streamInfo) {
        let hasStrippedAdSegments = false;
        const lines = textStr.replaceAll('\r', '').split('\n');
        const newAdUrl = 'https://twitch.tv';
        for (let i = 0; i < lines.length; i++) {
            let line = lines[i];
            line = line
                .replaceAll(/(X-TV-TWITCH-AD-URL=")(?:[^"]*)(")/g, `$1${newAdUrl}$2`)
                .replaceAll(/(X-TV-TWITCH-AD-CLICK-TRACKING-URL=")(?:[^"]*)(")/g, `$1${newAdUrl}$2`);
            if (i < lines.length - 1 && line.startsWith('#EXTINF') && (!line.includes(',live') || stripAllSegments || AllSegmentsAreAdSegments)) {
                const segmentUrl = lines[i + 1];
                if (!AdSegmentCache.has(segmentUrl)) {
                    streamInfo.NumStrippedAdSegments++;
                }
                AdSegmentCache.set(segmentUrl, Date.now());
                hasStrippedAdSegments = true;
            }
            if (line.includes(AdSignifier)) {
                hasStrippedAdSegments = true;
            }
        }
        if (hasStrippedAdSegments) {
            for (let i = 0; i < lines.length; i++) {
                if (lines[i].startsWith('#EXT-X-TWITCH-PREFETCH:')) {
                    lines[i] = '';
                }
            }
        } else {
            streamInfo.NumStrippedAdSegments = 0;
        }
        streamInfo.IsStrippingAdSegments = hasStrippedAdSegments;
        AdSegmentCache.forEach((value, key, map) => {
            if (value < Date.now() - 120000) {
                map.delete(key);
            }
        });
        return lines.join('\n');
    }

    function getStreamUrlForResolution(encodingsM3u8, resolutionInfo) {
        const encodingsLines = encodingsM3u8.replaceAll('\r', '').split('\n');
        const [targetWidth, targetHeight] = resolutionInfo.Resolution.split('x').map(Number);
        let matchedResolutionUrl = null;
        let matchedFrameRate = false;
        let closestResolutionUrl = null;
        let closestResolutionDifference = Infinity;
        for (let i = 0; i < encodingsLines.length - 1; i++) {
            if (encodingsLines[i].startsWith('#EXT-X-STREAM-INF') && encodingsLines[i + 1].includes('.m3u8')) {
                const attributes = parseAttributes(encodingsLines[i]);
                const resolution = attributes['RESOLUTION'];
                const frameRate = attributes['FRAME-RATE'];
                if (resolution) {
                    if (resolution == resolutionInfo.Resolution && (!matchedResolutionUrl || (!matchedFrameRate && frameRate == resolutionInfo.FrameRate))) {
                        matchedResolutionUrl = encodingsLines[i + 1];
                        matchedFrameRate = frameRate == resolutionInfo.FrameRate;
                        if (matchedFrameRate) {
                            return matchedResolutionUrl;
                        }
                    }
                    const [width, height] = resolution.split('x').map(Number);
                    const difference = Math.abs((width * height) - (targetWidth * targetHeight));
                    if (difference < closestResolutionDifference) {
                        closestResolutionUrl = encodingsLines[i + 1];
                        closestResolutionDifference = difference;
                    }
                }
            }
        }
        return closestResolutionUrl;
    }

    async function processM3U8(url, textStr, realFetch) {
        const streamInfo = StreamInfosByUrl[url];
        if (!streamInfo) return textStr;
        if (HasTriggeredPlayerReload) {
            HasTriggeredPlayerReload = false;
            streamInfo.LastPlayerReload = Date.now();
        }
        const haveAdTags = textStr.includes(AdSignifier) || SimulatedAdsDepth > 0;
        if (haveAdTags) {
            streamInfo.IsMidroll = textStr.includes('"MIDROLL"') || textStr.includes('"midroll"');
            if (!streamInfo.IsShowingAd) {
                streamInfo.IsShowingAd = true;
                postMessage({
                    key: 'UpdateAdBlockBanner',
                    isMidroll: streamInfo.IsMidroll,
                    hasAds: streamInfo.IsShowingAd,
                    isStrippingAdSegments: false
                });
            }
            const currentResolution = streamInfo.Urls[url];
            if (!currentResolution) return textStr;
            const isHevc = currentResolution.Codecs.startsWith('hev') || currentResolution.Codecs.startsWith('hvc');
            if (((isHevc && !SkipPlayerReloadOnHevc) || AlwaysReloadPlayerOnAd) && streamInfo.ModifiedM3U8 && !streamInfo.IsUsingModifiedM3U8) {
                streamInfo.IsUsingModifiedM3U8 = true;
                streamInfo.LastPlayerReload = Date.now();
                postMessage({ key: 'ReloadPlayer' });
            }
            let backupPlayerType = null;
            let backupM3u8 = null;
            let fallbackM3u8 = null;
            let startIndex = 0;
            if (streamInfo.LastPlayerReload > Date.now() - PlayerReloadMinimalRequestsTime) {
                startIndex = PlayerReloadMinimalRequestsPlayerIndex;
            }
            for (let playerTypeIndex = startIndex; !backupM3u8 && playerTypeIndex < BackupPlayerTypes.length; playerTypeIndex++) {
                const playerType = BackupPlayerTypes[playerTypeIndex];
                let encodingsM3u8 = streamInfo.BackupEncodingsM3U8Cache[playerType];
                if (!encodingsM3u8) {
                    try {
                        const accessTokenResponse = await getAccessToken(streamInfo.ChannelName, playerType);
                        if (accessTokenResponse.status === 200) {
                            const accessToken = await accessTokenResponse.json();
                            const urlInfo = new URL('https://usher.ttvnw.net/api/' + (V2API ? 'v2/' : '') + 'channel/hls/' + streamInfo.ChannelName + '.m3u8' + streamInfo.UsherParams);
                            urlInfo.searchParams.set('sig', accessToken.data.streamPlaybackAccessToken.signature);
                            urlInfo.searchParams.set('token', accessToken.data.streamPlaybackAccessToken.value);
                            const encodingsM3u8Response = await realFetch(urlInfo.href);
                            if (encodingsM3u8Response.status === 200) {
                                encodingsM3u8 = streamInfo.BackupEncodingsM3U8Cache[playerType] = await encodingsM3u8Response.text();
                            }
                        }
                    } catch (err) {}
                }
                if (encodingsM3u8) {
                    try {
                        const streamM3u8Url = getStreamUrlForResolution(encodingsM3u8, currentResolution);
                        const streamM3u8Response = await realFetch(streamM3u8Url);
                        if (streamM3u8Response.status == 200) {
                            const m3u8Text = await streamM3u8Response.text();
                            if (m3u8Text) {
                                if (playerType == FallbackPlayerType) fallbackM3u8 = m3u8Text;
                                if ((!m3u8Text.includes(AdSignifier) && (SimulatedAdsDepth == 0 || playerTypeIndex >= SimulatedAdsDepth - 1)) || playerTypeIndex >= BackupPlayerTypes.length - 1) {
                                    backupPlayerType = playerType;
                                    backupM3u8 = m3u8Text;
                                    break;
                                }
                            }
                        }
                    } catch (err) {}
                }
            }
            if (!backupM3u8 && fallbackM3u8) {
                backupPlayerType = FallbackPlayerType;
                backupM3u8 = fallbackM3u8;
            }
            if (backupM3u8) {
                textStr = backupM3u8;
                if (streamInfo.ActiveBackupPlayerType != backupPlayerType) {
                    streamInfo.ActiveBackupPlayerType = backupPlayerType;
                    console.log(`[Samtch] Blocking${(streamInfo.IsMidroll ? ' midroll ' : ' ')}ads (${backupPlayerType})`);
                }
            }
            const stripHevc = isHevc && streamInfo.ModifiedM3U8;
            if (IsAdStrippingEnabled || stripHevc) {
                textStr = stripAdSegments(textStr, stripHevc, streamInfo);
            }
        } else if (streamInfo.IsShowingAd) {
            console.log('[Samtch] Finished blocking ads');
            streamInfo.IsShowingAd = false;
            streamInfo.IsStrippingAdSegments = false;
            streamInfo.NumStrippedAdSegments = 0;
            streamInfo.ActiveBackupPlayerType = null;
            if (streamInfo.IsUsingModifiedM3U8 || ReloadPlayerAfterAd) {
                streamInfo.IsUsingModifiedM3U8 = false;
                streamInfo.LastPlayerReload = Date.now();
                postMessage({ key: 'ReloadPlayer' });
            } else {
                postMessage({ key: 'PauseResumePlayer' });
            }
        }
        postMessage({
            key: 'UpdateAdBlockBanner',
            isMidroll: streamInfo.IsMidroll,
            hasAds: streamInfo.IsShowingAd,
            isStrippingAdSegments: streamInfo.IsStrippingAdSegments,
            numStrippedAdSegments: streamInfo.NumStrippedAdSegments
        });
        return textStr;
    }

    function parseAttributes(str) {
        return Object.fromEntries(
            str.split(/(?:^|,)((?:[^=]*)=(?:"[^"]*"|[^,]*))/)
            .filter(Boolean)
            .map(x => {
                const idx = x.indexOf('=');
                const key = x.substring(0, idx);
                const value = x.substring(idx + 1);
                const num = Number(value);
                return [key, Number.isNaN(num) ? value.startsWith('"') ? JSON.parse(value) : value : num];
            }));
    }

    function getAccessToken(channelName, playerType) {
        const body = {
            operationName: 'PlaybackAccessToken',
            variables: {
                isLive: true,
                login: channelName,
                isVod: false,
                vodID: "",
                playerType: playerType,
                platform: playerType == 'autoplay' ? 'android' : 'web'
            },
            extensions: {
                persistedQuery: {
                    version:1,
                    sha256Hash:"ed230aa1e33e07eebb8928504583da78a5173989fadfb1ac94be06a04f3cdbe9"
                }
            }
        };
        return gqlRequest(body, playerType);
    }

    function gqlRequest(body, playerType) {
        if (!GQLDeviceID) {
            GQLDeviceID = '';
            const dcharacters = 'abcdefghijklmnopqrstuvwxyz0123456789';
            for (let i = 0; i < 32; i++) {
                GQLDeviceID += dcharacters.charAt(Math.floor(Math.random() * dcharacters.length));
            }
        }
        let headers = {
            'Client-ID': ClientID,
            'X-Device-Id': GQLDeviceID,
            'Authorization': AuthorizationHeader,
            ...(ClientIntegrityHeader && {'Client-Integrity': ClientIntegrityHeader}),
            ...(ClientVersion && {'Client-Version': ClientVersion}),
            ...(ClientSession && {'Client-Session-Id': ClientSession})
        };
        return new Promise((resolve, reject) => {
            const requestId = Math.random().toString(36).substring(2, 15);
            const fetchRequest = {
                id: requestId,
                url: 'https://gql.twitch.tv/gql',
                options: { method: 'POST', body: JSON.stringify(body), headers }
            };
            pendingFetchRequests.set(requestId, { resolve, reject });
            postMessage({ key: 'FetchRequest', value: fetchRequest });
        });
    }

    let playerForMonitoringBuffering = null;
    const playerBufferState = {
        channelName: null,
        hasStreamStarted: false,
        position: 0,
        bufferedPosition: 0,
        bufferDuration: 0,
        numSame: 0,
        lastFixTime: 0,
        isLive: true
    };

    function monitorPlayerBuffering() {
        if (playerForMonitoringBuffering) {
            try {
                const player = playerForMonitoringBuffering.player;
                const state = playerForMonitoringBuffering.state;
                if (!player.core) {
                    playerForMonitoringBuffering = null;
                } else if (state.props?.content?.type === 'live' && !player.isPaused() && !player.getHTMLVideoElement()?.ended && playerBufferState.lastFixTime <= Date.now() - PlayerBufferingMinRepeatDelay && !isActivelyStrippingAds) {
                    const m3u8Url = player.core?.state?.path;
                    if (m3u8Url) {
                        const fileName = new URL(m3u8Url).pathname.split('/').pop();
                        if (fileName?.endsWith('.m3u8')) {
                            const channelName = fileName.slice(0, -5);
                            if (playerBufferState.channelName != channelName) {
                                playerBufferState.channelName = channelName;
                                playerBufferState.hasStreamStarted = false;
                                playerBufferState.numSame = 0;
                            }
                        }
                    }
                    if (player.getState() === 'Playing') playerBufferState.hasStreamStarted = true;
                    const position = player.core?.state?.position;
                    const bufferedPosition = player.core?.state?.bufferedPosition;
                    const bufferDuration = player.getBufferDuration();
                    if (position !== undefined && bufferedPosition !== undefined) {
                        if (playerBufferState.hasStreamStarted &&
                            (!PlayerBufferingPrerollCheckEnabled || position > PlayerBufferingPrerollCheckOffset) &&
                            (playerBufferState.position == position || bufferDuration < PlayerBufferingDangerZone)  &&
                            playerBufferState.bufferedPosition == bufferedPosition &&
                            playerBufferState.bufferDuration >= bufferDuration &&
                            (position != 0 || bufferedPosition != 0 || bufferDuration != 0)
                        ) {
                            playerBufferState.numSame++;
                            if (playerBufferState.numSame == PlayerBufferingSameStateCount) {
                                console.log('[Samtch] Fixing buffering at pos:' + playerBufferState.position);
                                doTwitchPlayerTask(!PlayerBufferingDoPlayerReload, PlayerBufferingDoPlayerReload);
                                playerBufferState.lastFixTime = Date.now();
                                playerBufferState.numSame = 0;
                            }
                        } else {
                            playerBufferState.numSame = 0;
                        }
                        playerBufferState.position = position;
                        playerBufferState.bufferedPosition = bufferedPosition;
                        playerBufferState.bufferDuration = bufferDuration;
                    } else {
                        playerBufferState.numSame = 0;
                    }
                }
            } catch (err) {
                playerForMonitoringBuffering = null;
            }
        }
        if (!playerForMonitoringBuffering) {
            const playerAndState = getPlayerAndState();
            if (playerAndState && playerAndState.player && playerAndState.state) {
                playerForMonitoringBuffering = { player: playerAndState.player, state: playerAndState.state };
            }
        }
        const isLive = playerForMonitoringBuffering?.state?.props?.content?.type === 'live';
        if (playerBufferState.isLive && !isLive) {
            updateAdblockBanner({ hasAds: false });
        }
        playerBufferState.isLive = isLive;
        window.samtch_vaft_monitor_timer = setTimeout(monitorPlayerBuffering, PlayerBufferingDelay);
    }

    function updateAdblockBanner(data) {
        const playerRootDiv = document.querySelector('.video-player') || document.querySelector('[data-a-target="video-player"]');
        if (playerRootDiv != null) {
            let adBlockDiv = playerRootDiv.querySelector('.adblock-overlay');
            if (adBlockDiv == null) {
                adBlockDiv = document.createElement('div');
                adBlockDiv.className = 'adblock-overlay';
                adBlockDiv.innerHTML = '<div class="player-adblock-notice" style="color: white; background-color: rgba(0, 0, 0, 0.8); position: absolute; top: 0px; left: 0px; padding: 5px; z-index: 9999;"><p></p></div>';
                adBlockDiv.style.display = 'none';
                adBlockDiv.P = adBlockDiv.querySelector('p');
                playerRootDiv.appendChild(adBlockDiv);
            }
            if (adBlockDiv != null) {
                isActivelyStrippingAds = data.isStrippingAdSegments;
                adBlockDiv.P.textContent = 'Blocking' + (data.isMidroll ? ' midroll' : '') + ' ads' + (data.isStrippingAdSegments ? ' (stripping)' : '');
                adBlockDiv.style.display = data.hasAds && playerBufferState.isLive ? 'block' : 'none';
            }
        }
    }

    function getPlayerAndState() {
        function findReactNode(root, constraint) {
            if (root.stateNode && constraint(root.stateNode)) return root.stateNode;
            let node = root.child;
            while (node) {
                const result = findReactNode(node, constraint);
                if (result) return result;
                node = node.sibling;
            }
            return null;
        }
        function findReactRootNode() {
            const rootNode = document.querySelector('#root');
            if (rootNode && rootNode._reactRootContainer && rootNode._reactRootContainer._internalRoot && rootNode._reactRootContainer._internalRoot.current) {
                return rootNode._reactRootContainer._internalRoot.current;
            }
            if (rootNode) {
                const containerName = Object.keys(rootNode).find(x => x.startsWith('__reactContainer'));
                if (containerName) return rootNode[containerName];
            }
            return null;
        }
        const reactRootNode = findReactRootNode();
        if (!reactRootNode) return null;
        let player = findReactNode(reactRootNode, node => node.setPlayerActive && node.props && node.props.mediaPlayerInstance);
        player = player && player.props && player.props.mediaPlayerInstance ? player.props.mediaPlayerInstance : null;
        if (player?.playerInstance) player = player.playerInstance;
        const playerState = findReactNode(reactRootNode, node => node.setSrc && node.setInitialPlaybackSettings);
        return { player: player, state: playerState };
    }

    function doTwitchPlayerTask(isPausePlay, isReload) {
        const playerAndState = getPlayerAndState();
        if (!playerAndState) return;
        const { player, state: playerState } = playerAndState;
        if (!player || !playerState || player.isPaused() || player.core?.paused) return;

        playerBufferState.lastFixTime = Date.now();
        playerBufferState.numSame = 0;
        if (isPausePlay) {
            player.pause();
            player.play();
            return;
        }
        if (isReload) {
            console.log('[Samtch] Reloading Twitch player');
            playerState.setSrc({ isNewMediaPlayerInstance: true, refreshAccessToken: true });
            postTwitchWorkerMessage('TriggeredPlayerReload');
            player.play();
        }
    }

    window.reloadTwitchPlayer = () => doTwitchPlayerTask(false, true);

    function postTwitchWorkerMessage(key, value) {
        twitchWorkers.forEach((worker) => worker.postMessage({key: key, value: value}));
    }

    async function handleWorkerFetchRequest(fetchRequest) {
        try {
            const response = await window.realFetch(fetchRequest.url, fetchRequest.options);
            const responseBody = await response.text();
            return {
                id: fetchRequest.id,
                status: response.status,
                statusText: response.statusText,
                headers: Object.fromEntries(response.headers.entries()),
                body: responseBody
            };
        } catch (error) {
            return { id: fetchRequest.id, error: error.message };
        }
    }

    function hookFetch() {
        if (window.fetch && window.fetch.samtch_hooked) return;
        const realFetch = window.fetch;
        window.realFetch = realFetch;
        window.fetch = function(url, init, ...args) {
            if (typeof url === 'string' && url.includes('gql')) {
                let deviceId = init.headers['X-Device-Id'] || init.headers['Device-ID'];
                if (typeof deviceId === 'string' && GQLDeviceID != deviceId) {
                    GQLDeviceID = deviceId;
                    postTwitchWorkerMessage('UpdateDeviceId', GQLDeviceID);
                }
                if (init.headers['Client-Version'] && init.headers['Client-Version'] !== ClientVersion) {
                    postTwitchWorkerMessage('UpdateClientVersion', ClientVersion = init.headers['Client-Version']);
                }
                if (init.headers['Client-Session-Id'] && init.headers['Client-Session-Id'] !== ClientSession) {
                    postTwitchWorkerMessage('UpdateClientSession', ClientSession = init.headers['Client-Session-Id']);
                }
                if (init.headers['Client-Integrity'] && init.headers['Client-Integrity'] !== ClientIntegrityHeader) {
                    postTwitchWorkerMessage('UpdateClientIntegrityHeader', ClientIntegrityHeader = init.headers['Client-Integrity']);
                }
                if (init.headers['Authorization'] && init.headers['Authorization'] !== AuthorizationHeader) {
                    postTwitchWorkerMessage('UpdateAuthorizationHeader', AuthorizationHeader = init.headers['Authorization']);
                }
                if (init && typeof init.body === 'string' && init.body.includes('PlaybackAccessToken') && init.body.includes('picture-by-picture')) {
                    init.body = '';
                }
                if (ForceAccessTokenPlayerType && typeof init.body === 'string' && init.body.includes('PlaybackAccessToken')) {
                    const newBody = JSON.parse(init.body);
                    let replaced = false;
                    const items = Array.isArray(newBody) ? newBody : [newBody];
                    items.forEach(item => {
                        if (item?.variables?.playerType && item.variables.playerType !== ForceAccessTokenPlayerType) {
                            item.variables.playerType = ForceAccessTokenPlayerType;
                            replaced = true;
                        }
                    });
                    if (replaced) init.body = JSON.stringify(newBody);
                }
            }
            return realFetch.apply(this, arguments);
        };
        window.fetch.samtch_hooked = true;
    }

    function onContentLoaded() {
        try {
            Object.defineProperty(document, 'visibilityState', { get() { return 'visible'; } });
            Object.defineProperty(document, 'hidden', { get() { return false; } });
        } catch {}

        const visibilityChange = e => {
            const videos = document.getElementsByTagName('video');
            if (videos.length > 0 && !document.hidden && videos[0].paused && !videos[0].ended && videos[0].muted) {
                videos[0].play();
            }
            e.preventDefault(); e.stopPropagation(); e.stopImmediatePropagation();
        };
        document.addEventListener('visibilitychange', visibilityChange, true);
        document.addEventListener('webkitvisibilitychange', visibilityChange, true);
    }

    declareOptions(window);
    hookWindowWorker();
    hookFetch();
    if (PlayerBufferingFix) monitorPlayerBuffering();

    if (document.readyState === "complete" || document.readyState === "loaded" || document.readyState === "interactive") {
        onContentLoaded();
    } else {
        window.addEventListener("DOMContentLoaded", onContentLoaded);
    }
})();
