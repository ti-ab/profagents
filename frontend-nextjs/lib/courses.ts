
export type BookDTO = { id: number; title: string; authors?: string | null };
export type GenerateRequest = { description: string; title?: string; authors?: string };

const COURSE_API = process.env.NEXT_PUBLIC_COURSE_API || 'http://localhost:8087';
import { getBearerToken } from '@/lib/authClient';

export async function listBooksOnly() : Promise<BookDTO[]> {
  const res = await fetch(`${COURSE_API}/api/booksOnly`, { cache: 'no-store', headers: await authHeaders() });
  if (!res.ok) throw new Error('Failed to list books');
  return res.json();
}

export async function getBook(id: number) {
  const res = await fetch(`${COURSE_API}/api/books/${id}`, { cache: 'no-store', headers: await authHeaders() });
  if (!res.ok) throw new Error('Failed to get book');
  return res.json();
}

export async function generateBook(req: GenerateRequest) : Promise<BookDTO> {
  const res = await fetch(`${COURSE_API}/api/books/generate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(await authHeaders()) },
    body: JSON.stringify(req),
  });
  if (!res.ok) throw new Error('Failed to generate book');
  return res.json();
}

export async function updateBook(id: number, data: Partial<BookDTO>) : Promise<BookDTO> {
  const res = await fetch(`${COURSE_API}/api/books/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...(await authHeaders()) },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error('Failed to update book');
  return res.json();
}

export async function deleteBook(id: number) {
  const res = await fetch(`${COURSE_API}/api/books/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error('Failed to delete book');
}


async function authHeaders() {
  const token = await getBearerToken();
  return token ? { 'Authorization': `Bearer ${token}` } : {};
}
