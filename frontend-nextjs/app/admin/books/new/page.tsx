
'use client';

import { useState } from 'react';
import { generateBook } from '@/lib/courses';
import { useRouter } from 'next/navigation';

export default function NewBookPage() {
  const [description, setDescription] = useState('Apprendre l\'anglais pour les débutants');
  const [title, setTitle] = useState('');
  const [authors, setAuthors] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      if (description.trim().length < 20) throw new Error('Merci de détailler la description (≥ 20 caractères)');
      const res = await generateBook({ description, title: title || undefined, authors: authors || undefined });
      router.replace(`/admin/books/${res.id}`);
    } catch (e:any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="p-6 max-w-2xl space-y-4">
      <h1 className="text-2xl font-semibold">Generate a new Book</h1>
      <form onSubmit={onSubmit} className="space-y-3">
        <div className="space-y-1">
          <label className="text-sm">Book description</label>
          <textarea className="w-full border rounded-xl p-2 min-h-[120px]" value={description} onChange={e=>setDescription(e.target.value)} required />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1">
            <label className="text-sm">Title (optional)</label>
            <input className="w-full border rounded-xl p-2" value={title} onChange={e=>setTitle(e.target.value)} />
          </div>
          <div className="space-y-1">
            <label className="text-sm">Authors (optional)</label>
            <input className="w-full border rounded-xl p-2" value={authors} onChange={e=>setAuthors(e.target.value)} />
          </div>
        </div>
        <button disabled={loading} className="rounded-2xl px-4 py-2 border shadow">{loading ? 'Generating…' : 'Generate'}</button>
        {error && <div className="text-red-600 text-sm">{error}</div>}
      
        <div className="h-2 bg-gray-200 rounded overflow-hidden">
          <div className="h-2 transition-all" style={{ width: loading ? '80%' : '0%' }} />
        </div>
      </form>
      <p className="text-xs opacity-70">Assurez-vous que <code>NEXT_PUBLIC_COURSE_API</code> pointe vers le course-service (ex: http://localhost:8087).</p>
    </div>
  );
}
