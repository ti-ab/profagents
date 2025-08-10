
'use client';

import { useEffect, useState } from 'react';
import { getBook, updateBook } from '@/lib/courses';
import { useParams, useRouter } from 'next/navigation';

export default function EditBookPage() {
  const params = useParams();
  const id = Number(params?.id);
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [title, setTitle] = useState('');
  const [authors, setAuthors] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [bookData, setBookData] = useState<any>(null);

  useEffect(() => {
    (async () => {
      try {
        const data = await getBook(id);
        setBookData(data);
        setTitle(data.title || '');
        setAuthors(data.authors || '');
      } catch (e:any) {
        setError(e.message);
      } finally {
        setLoading(false);
      }
    })();
  }, [id]);

  async function onSave() {
    try {
      await updateBook(id, { title, authors });
      router.refresh();
      alert('Saved ✓');
    } catch (e:any) {
      setError(e.message);
    }
  }

  if (loading) return <div className="p-6">Loading…</div>;

  return (
    <div className="p-6 max-w-2xl space-y-4">
      <h1 className="text-2xl font-semibold">Edit Book</h1>
      {error && <div className="text-red-600">{error}</div>}
      <div className="space-y-2">
        <label className="text-sm">Title</label>
        <input className="w-full border rounded-xl p-2" value={title} onChange={e=>setTitle(e.target.value)} />
      </div>
      <div className="space-y-2">
        <label className="text-sm">Authors</label>
        <input className="w-full border rounded-xl p-2" value={authors} onChange={e=>setAuthors(e.target.value)} />
      </div>
      <button onClick={onSave} className="rounded-2xl px-4 py-2 border shadow">Save</button>
    
      <h2 className="text-xl font-semibold mt-6">Contenu</h2>
      {bookData ? <TreeView data={bookData} /> : <div className="text-sm opacity-70">Aucune donnée.</div>}
    </div>
  );
}

function TreeView({ data }: { data: any }) {
  return (
    <div className="mt-2">
      {(data.chapters || []).map((ch: any) => (
        <details key={ch.id} className="mb-2">
          <summary className="cursor-pointer font-medium">Chapter {ch.idx}: {ch.title}</summary>
          <div className="ml-4 mt-2">
            {(ch.subchapters || []).map((sc: any) => (
              <details key={sc.id} className="mb-2">
                <summary className="cursor-pointer">Subchapter {sc.idx}: {sc.title}</summary>
                <ul className="ml-6 list-disc">
                  {(sc.sections || []).map((s: any) => (
                    <li key={s.id} className="py-0.5"><span className="font-mono text-xs">#{s.idx}</span> {s.title}</li>
                  ))}
                </ul>
              </details>
            ))}
          </div>
        </details>
      ))}
    </div>
  );
}

