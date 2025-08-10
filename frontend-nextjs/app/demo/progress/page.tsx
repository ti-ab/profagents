
'use client';

import { useState } from 'react';
import { saveProgress, type ProgressPayload } from '@/lib/progress';

export default function DemoProgressPage() {
  const [status, setStatus] = useState<string>('');
  const [loading, setLoading] = useState(false);

  async function handleClick() {
    setLoading(true);
    try {
      const payload: ProgressPayload = {
        userId: 1,
        bookId: 42,
        chapterId: 3,
        sectionId: 9,
        sectionTime: 540,
        sectionRating: 4.5,
        startTimestamp: new Date(Date.now() - 540_000).toISOString(),
        endTimestamp: new Date().toISOString(),
      };
      await saveProgress('', payload); // supply JWT in real app
      setStatus('Progress saved ✓');
    } catch (e:any) {
      setStatus('Error: ' + e.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="p-6 max-w-xl space-y-4">
      <h1 className="text-2xl font-semibold">Demo: Save Progress</h1>
      <p className="text-sm opacity-80">Clicks will post to <code>/api/progress</code> of user-service.</p>
      <button onClick={handleClick} disabled={loading} className="rounded-2xl px-4 py-2 border shadow">
        {loading ? 'Saving...' : 'Save a sample progress event'}
      </button>
      <div className="text-sm">{status}</div>
    </div>
  );
}
