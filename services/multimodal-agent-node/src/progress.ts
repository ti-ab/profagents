
export type ProgressPayload = {
  userId: number;
  bookId: number;
  chapterId?: number;
  subchapterId?: number;
  sectionId?: number;
  sectionRating?: number;
  sectionTime?: number;
  startTimestamp?: string;
  endTimestamp?: string;
};

const USER_API = process.env.USER_SERVICE_URL || 'http://user-service:8081';

export async function postProgress(payload: ProgressPayload, token?: string) {
  const res = await fetch(`${USER_API}/api/progress`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`postProgress failed: ${res.status} ${text}`);
  }
  return res.json();
}
