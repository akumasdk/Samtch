(function betterttv() {
    var script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = 'https://cdn.betterttv.net/betterttv.js';
    var head = document.getElementsByTagName('head')[0];
    if (!head) return;
    head.appendChild(script);
})()