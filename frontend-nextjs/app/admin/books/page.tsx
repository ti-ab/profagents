
'use client';

import { useEffect, useState } from 'react';
import { listBooksOnly, deleteBook, type BookDTO } from '@/lib/courses';
import Link from 'next/link';

export default function BooksAdmin() {
  const [books, setBooks] = useState<BookDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function refresh() {
    try {
      setLoading(true);
      setBooks(await listBooksOnly());
    } catch (e:any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { refresh(); }, []);

  async function onDelete(id: number) {
    if (!confirm('Supprimer ce book ?')) return;
    await deleteBook(id);
    await refresh();
  }

  return (
    <div className="p-6 space-y-4 max-w-3xl">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Books (Courses)</h1>
        <Link href="/admin/books/new" className="rounded-2xl px-4 py-2 border shadow">+ New (Generate)</Link>
      </div>
      {loading && <div>Loading…</div>}
      {error && <div className="text-red-600">{error}</div>}
      {!loading && books.length === 0 && <div>Aucun book pour l’instant.</div>}
      <ul className="divide-y">
        {books.map(b => (
          <li key={b.id} className="py-3 flex items-center justify-between">
            <div>
              <div className="font-medium">{b.title}</div>
              <div className="text-sm opacity-70">{b.authors || '—'}</div>
            </div>
            <div className="flex gap-2">
              <Link href={`/admin/books/${b.id}`} className="rounded-xl px-3 py-1 border">Edit</Link>
              <button onClick={() => onDelete(b.id)} className="rounded-xl px-3 py-1 border">Delete</button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
