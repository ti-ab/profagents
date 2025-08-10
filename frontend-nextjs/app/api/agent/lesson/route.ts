
import { NextRequest, NextResponse } from 'next/server';

const COURSE_API = process.env.NEXT_PUBLIC_COURSE_API || process.env.COURSE_API || 'http://course-service:8087';
const AGENT_API = process.env.NEXT_PUBLIC_AGENT_API || process.env.AGENT_API || 'http://multimodal-agent-node:8084';

export async function POST(req: NextRequest) {
  try {
    const { courseId, sectionId } = await req.json();
    if (!courseId || !sectionId) {
      return NextResponse.json({ error: 'courseId and sectionId are required' }, { status: 400 });
    }
    // Fetch course full tree
    const courseRes = await fetch(`${COURSE_API}/api/books/${courseId}`, { cache: 'no-store' });
    if (!courseRes.ok) {
      return NextResponse.json({ error: 'Failed to fetch course' }, { status: 502 });
    }
    const book = await courseRes.json();
    // Find section content
    let text: string | null = null;
    for (const ch of (book.chapters || [])) {
      for (const sc of (ch.subchapters || [])) {
        for (const s of (sc.sections || [])) {
          if (s.id === sectionId || String(s.id) === String(sectionId)) {
            text = s.content || s.title;
            break;
          }
        }
      }
    }
    if (!text) {
      return NextResponse.json({ error: 'Section not found' }, { status: 404 });
    }
    // Send to agent
    const res2 = await fetch(`${AGENT_API}/context/lesson`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text }),
    });
    if (!res2.ok) {
      const t = await res2.text();
      return NextResponse.json({ error: 'Failed to push lesson', details: t }, { status: 502 });
    }
    return NextResponse.json({ ok: true });
  } catch (e:any) {
    return NextResponse.json({ error: e.message || 'Unexpected error' }, { status: 500 });
  }
}
