
export type ProgressPayload = {
  userId: number
  bookId: number
  chapterId?: number
  subchapterId?: number
  sectionId?: number
  sectionRating?: number
  sectionTime?: number
  startTimestamp?: string
  endTimestamp?: string
}

const USER_API = process.env.NEXT_PUBLIC_USER_API || 'http://localhost:8081';

export async function saveProgress(token: string, payload: ProgressPayload) {
  const res = await fetch(`${USER_API}/api/progress`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(payload),
    cache: 'no-store',
  });
  if (!res.ok) {
    const txt = await res.text();
    throw new Error(`Progress save failed: ${res.status} ${txt}`);
  }
  return res.json();
}

export async function getProgress(token: string, userId: number, bookId?: number) {
  const url = new URL(`${USER_API}/api/progress/${userId}`);
  if (bookId) url.searchParams.set('bookId', String(bookId));
  const res = await fetch(url.toString(), {
    headers: token ? { 'Authorization': `Bearer ${token}` } : {},
    cache: 'no-store',
  });
  if (!res.ok) throw new Error('Failed to fetch progress');
  return res.json();
}
