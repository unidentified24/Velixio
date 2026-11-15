'use strict';

const video = document.getElementById('video');
const stage = document.getElementById('stage');
const status = document.getElementById('status');
const full = document.getElementById('full');
const reconnect = document.getElementById('reconnect');
const fit = document.getElementById('fit');
const quality = document.getElementById('quality');
const qualityLabel = document.getElementById('quality-label');
const media = video.dataset.media;
const isHls = video.dataset.hls === 'true';
const nativeHls = isHls && !!video.canPlayType('application/vnd.apple.mpegurl');
const libraryHls = isHls && typeof window.Hls !== 'undefined' && window.Hls.isSupported();
const start = Number(video.dataset.start);
let resumeTime = Number.isFinite(start) ? start : 0;
let wantPlay = false;
let player;
let mode;
let recovered;
let triedFallback;
let failed;

function problem(message) {
    failed = true;
    status.textContent = message;
    reconnect.hidden = false;
}

function savePosition() {
    if (video.readyState > 0 && Number.isFinite(video.currentTime)) resumeTime = video.currentTime;
}

function seekToStart() {
    if (!(resumeTime > 0)) return;
    const seekable = video.seekable;
    if (seekable.length) {
        for (let i = 0; i < seekable.length; i++) {
            if (resumeTime >= seekable.start(i) && resumeTime <= seekable.end(i)) {
                video.currentTime = resumeTime;
                return;
            }
        }
    } else if (Number.isFinite(video.duration) && resumeTime < video.duration) {
        video.currentTime = resumeTime;
    }
}

function reset() {
    if (player) player.destroy();
    player = undefined;
    mode = undefined;
    video.removeAttribute('src');
    video.load();
    quality.options.length = 1;
    quality.value = '-1';
    quality.hidden = qualityLabel.hidden = true;
}

function playWhenReady() {
    if (wantPlay) video.play().catch(() => {
        if (!failed) status.textContent = 'Press Play to continue.';
    });
}

function alternate() {
    if (triedFallback) return false;
    if ((mode === 'library' && nativeHls) || (mode === 'native' && libraryHls)) {
        const next = mode === 'library' ? 'native' : 'library';
        triedFallback = true;
        savePosition();
        open(next);
        return true;
    }
    return false;
}

function open(method) {
    reset();
    mode = method;
    failed = false;
    recovered = false;
    reconnect.hidden = true;
    status.textContent = 'Loading from your phone…';
    if (method === 'native') {
        video.src = media;
        return;
    }
    const Hls = window.Hls;
    const instance = new Hls({ enableWorker: false });
    player = instance;
    instance.on(Hls.Events.MEDIA_ATTACHED, () => instance.loadSource(media));
    instance.on(Hls.Events.MANIFEST_PARSED, () => {
        const levels = instance.levels || [];
        levels.forEach((level, index) => {
            const size = level.height ? `${level.height}p` : `Stream ${index + 1}`;
            const bandwidth = level.bitrate ? ` · ${(level.bitrate / 1000000).toFixed(1)} Mbps` : '';
            quality.add(new Option(size + bandwidth, String(index)));
        });
        quality.hidden = qualityLabel.hidden = levels.length < 2;
    });
    instance.on(Hls.Events.ERROR, (_, error) => {
        if (instance !== player || !error.fatal) return;
        if (error.type === Hls.ErrorTypes.NETWORK_ERROR) {
            const code = error.response && error.response.code;
            if (Hls.ErrorDetails && error.details === Hls.ErrorDetails.MANIFEST_PARSING_ERROR) {
                problem('The browser received an unreadable playlist. Choose another source on your phone.');
            } else if (code === 502 || code === 503) {
                problem('The phone could not load this stream. Open the title on your phone, then reconnect or choose another source.');
            } else if (code === 404 || code === 410) {
                problem('This casting session expired. Pair again from the phone.');
            } else {
                problem('The browser lost the stream. Keep your phone on this network and select Reconnect.');
            }
            return;
        }
        const incompatible = Hls.ErrorDetails && error.details === Hls.ErrorDetails.MANIFEST_INCOMPATIBLE_CODECS_ERROR;
        if (error.type === Hls.ErrorTypes.MEDIA_ERROR && !recovered && !incompatible) {
            recovered = true;
            savePosition();
            instance.recoverMediaError();
            return;
        }
        if (!alternate()) problem('This browser could not decode the selected video or audio. Choose another source on your phone or use screen mirroring.');
    });
    instance.attachMedia(video);
}

function connect() {
    triedFallback = false;
    if (libraryHls) open('library');
    else if (!isHls || nativeHls) open('native');
    else problem('This browser needs native HLS or Media Source support to play this stream.');
}

fit.addEventListener('change', () => { video.dataset.fit = fit.value; });
quality.addEventListener('change', () => { if (player) player.currentLevel = Number(quality.value); });
reconnect.addEventListener('click', () => { savePosition(); wantPlay = true; connect(); });

function toggleFullscreen() {
    if (document.fullscreenElement) document.exitFullscreen().catch(() => {});
    else if (stage.requestFullscreen) stage.requestFullscreen().catch(() => {
        if (video.webkitEnterFullscreen) video.webkitEnterFullscreen();
    });
    else if (video.webkitEnterFullscreen) video.webkitEnterFullscreen();
}

full.addEventListener('click', toggleFullscreen);
full.hidden = !stage.requestFullscreen && !video.webkitEnterFullscreen;
document.addEventListener('fullscreenchange', () => {
    full.textContent = document.fullscreenElement ? 'Exit full screen' : 'Full screen';
});
document.addEventListener('keydown', event => {
    if (event.defaultPrevented || event.ctrlKey || event.metaKey || event.altKey) return;
    if (event.target && (event.target.isContentEditable || /^(INPUT|SELECT|BUTTON|TEXTAREA|VIDEO)$/.test(event.target.tagName))) return;
    if (event.key.toLowerCase() === 'f' && !full.hidden) toggleFullscreen();
    else if (event.key === ' ') {
        if (video.paused) video.play().catch(() => {});
        else video.pause();
    } else if (Number.isFinite(video.duration) && (event.key === 'ArrowLeft' || event.key === 'ArrowRight')) {
        const step = event.key === 'ArrowLeft' ? -10 : 10;
        video.currentTime = Math.max(0, Math.min(video.duration, video.currentTime + step));
    } else return;
    event.preventDefault();
});

video.addEventListener('loadedmetadata', () => {
    if (failed) return;
    seekToStart();
    status.textContent = 'Ready. Press Play to watch.';
    playWhenReady();
});
video.addEventListener('play', () => { wantPlay = true; });
video.addEventListener('pause', () => { if (video.readyState > 0 && !failed) wantPlay = false; });
video.addEventListener('playing', () => {
    failed = false;
    reconnect.hidden = true;
    status.textContent = 'Playing. You can lock your phone while it stays on this network.';
});
video.addEventListener('timeupdate', () => { if (mode && !failed) savePosition(); });
video.addEventListener('waiting', () => { if (!failed) status.textContent = 'Buffering from your phone…'; });
video.addEventListener('error', () => {
    if (mode !== 'native' || failed || !video.error) return;
    if (alternate()) return;
    if (video.error.code === 2) problem('The stream connection was interrupted. Check your phone and reconnect.');
    else problem('This browser could not open the selected stream. Choose another source on your phone or use screen mirroring.');
});
window.addEventListener('pagehide', reset, { once: true });
connect();
