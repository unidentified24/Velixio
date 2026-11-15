package app.veyra;

import java.util.Locale;

final class BrowserReceiver {
    private BrowserReceiver() {}

    static String page(String media, String mime, long position, boolean bundledHls) {
        boolean hls = "application/x-mpegURL".equals(mime);
        return String.format(Locale.ROOT, """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Velixio · Browser playback</title>
              <link rel="stylesheet" href="receiver.css">
            </head>
            <body>
              <main>
                <header><strong>Velixio</strong><span>Browser playback</span></header>
                <section id="stage" aria-label="Video player">
                  <video id="video" controls playsinline preload="metadata" data-fit="contain"
                         data-media="%s" data-hls="%s" data-start="%.3f"></video>
                  <div class="toolbar" role="group" aria-label="Viewing controls">
                    <label for="fit">Picture size</label>
                    <select id="fit">
                      <option value="contain">Fit</option>
                      <option value="cover">Fill (crop)</option>
                      <option value="fill">Stretch</option>
                    </select>
                    <label id="quality-label" for="quality" hidden>Quality</label>
                    <select id="quality" hidden><option value="-1">Auto</option></select>
                    <button id="full" type="button">Full screen</button>
                    <button id="reconnect" type="button" hidden>Reconnect</button>
                  </div>
                  <p id="status" role="status">Connecting to your phone…</p>
                </section>
                <footer>Space plays or pauses. Left and right arrows skip 10 seconds. F toggles full screen.</footer>
              </main>
              %s
              <script src="receiver.js"></script>
            </body>
            </html>
            """, escape(media), hls, Math.max(0,position)/1000.0,
            hls && bundledHls ? "<script src=\"hls.min.js\"></script>" : "");
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;")
            .replace("<", "&lt;").replace(">", "&gt;");
    }
}
