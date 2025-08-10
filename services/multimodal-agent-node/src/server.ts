// server.ts
import express from 'express';
import { setLessonText } from './lessonContext.js';

// @ts-ignore – mémorise le serveur au niveau process
declare global { var __lessonServerStarted: boolean | undefined; }



export function ensureServer() {
    if (globalThis.__lessonServerStarted) return;

    const app = express();
    app.use(express.json({ limit: '2mb' }));

    app.post('/context/lesson', (req: any, res: any) => {
        const text = req?.body?.text;
        if (typeof text !== 'string' || !text.trim()) {
            return res.status(400).json({ error: 'text is required' });
        }
        setLessonText(text);
        return res.json({ ok: true });
    });

    const PORT = Number(process.env.AGENT_PORT || process.env.PORT || 8084);
    const server = app.listen(PORT, () => {
        console.log(`[agent] HTTP context server listening on :${PORT}`);
        globalThis.__lessonServerStarted = true;
    });
// Si un autre process possède déjà le port, ne pas crasher le worker
    server.on('error', (err: any) => {
        if (err && (err as any).code === 'EADDRINUSE') {
            console.warn(`[agent] context server port ${PORT} already in use; assuming another process started it. Skipping.`);
            globalThis.__lessonServerStarted = true;
            return;
        }
        throw err;
    });


    globalThis.__lessonServerStarted = true;
}

