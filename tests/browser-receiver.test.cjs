const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname,
    '../app/src/main/assets/cast-browser/receiver.js'), 'utf8');

function element(properties = {}) {
    const listeners = new Map();
    return {
        hidden: false,
        addEventListener(name, listener) { listeners.set(name, listener); },
        emit(name, event) { listeners.get(name)(event); },
        ...properties,
    };
}

function start({ m4s = true, native = '', adaptive = true } = {}) {
    const instances = [];
    let fullscreen = false;
    const video = element({
        dataset: { start: '4.5', hls: String(adaptive), media: 'fixture.m3u8' },
        duration: 120, seekable: { length: 0 }, readyState: 1, currentTime: 0,
        canPlayType: () => native, load() {},
        removeAttribute() { delete this.src; },
        play: async function () { this.paused = false; },
        pause() { this.paused = true; },
    });
    const controls = {
        video, status: element(), full: element(), reconnect: element(),
        fit: element(), quality: element({ options: [], add(option) { this.options.push(option); } }),
        'quality-label': element(),
        stage: element({ requestFullscreen: async () => { fullscreen = true; } }),
    };
    class Hls {
        static isSupported() { return m4s; }
        static Events = { MEDIA_ATTACHED: 'attached', MANIFEST_PARSED: 'manifest', ERROR: 'error' };
        static ErrorTypes = { NETWORK_ERROR: 'network', MEDIA_ERROR: 'media' };
        static ErrorDetails = { MANIFEST_INCOMPATIBLE_CODECS_ERROR: 'codec', MANIFEST_PARSING_ERROR: 'parse' };
        constructor() { this.listeners = new Map(); instances.push(this); }
        on(name, listener) { this.listeners.set(name, listener); }
        emit(name, value) { this.listeners.get(name)(name, value); }
        attachMedia() { this.emit('attached'); }
        loadSource(url) { this.source = url; }
        destroy() { this.destroyed = true; }
        recoverMediaError() { this.recoveryCalls = (this.recoveryCalls || 0) + 1; }
    }
    const document = element({ getElementById: id => controls[id] });
    vm.runInNewContext(source, { document, window: element({ Hls }), Option: function (text, value) {
        this.text = text;
        this.value = value;
    } });
    return { ...controls, instances, document, fullscreen: () => fullscreen };
}

const pc = start({ native: 'maybe' });
const player = pc.instances[0];
assert.equal(player.source, 'fixture.m3u8');
player.levels = [{ height: 480, bitrate: 800000 }, { height: 1080, bitrate: 4200000 }];
player.emit('manifest');
assert.equal(pc.quality.hidden, false);
pc.quality.value = '1';
pc.quality.emit('change');
assert.equal(player.currentLevel, 1);
pc.video.emit('loadedmetadata');
assert.equal(pc.video.currentTime, 4.5);
for (const fit of ['cover', 'fill', 'contain']) {
    pc.fit.value = fit;
    pc.fit.emit('change');
    assert.equal(pc.video.dataset.fit, fit);
}
pc.full.emit('click');
assert.equal(pc.fullscreen(), true);
pc.document.emit('keydown', { key: 'ArrowRight', preventDefault() {} });
assert.equal(pc.video.currentTime, 14.5);
player.emit('error', { fatal: true, type: 'network', response: { code: 502 } });
assert.match(pc.status.textContent, /phone could not load/);
assert.equal(pc.reconnect.hidden, false);
pc.video.emit('waiting');
assert.match(pc.status.textContent, /phone could not load/);
pc.reconnect.emit('click');
assert.equal(player.destroyed, true);
assert.equal(pc.instances[1].source, 'fixture.m3u8');
pc.video.currentTime = 0;
pc.video.emit('loadedmetadata');
assert.equal(pc.video.currentTime, 14.5);

const first = start({ native: 'maybe' });
const decoder = first.instances[0];
decoder.emit('error', { fatal: true, type: 'media' });
assert.equal(decoder.recoveryCalls, 1);
decoder.emit('error', { fatal: true, type: 'media' });
assert.equal(first.video.src, 'fixture.m3u8');
assert.equal(decoder.destroyed, true);

assert.equal(start({ m4s: false, native: 'maybe' }).video.src, 'fixture.m3u8');
assert.equal(start({ m4s: false, adaptive: false }).video.src, 'fixture.m3u8');
assert.match(start({ m4s: false }).status.textContent, /Media Source support/);
console.log('Browser receiver checks passed for playback modes, fallback, retry, fit, quality, and keyboard controls.');
