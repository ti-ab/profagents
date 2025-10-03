import type {NextApiRequest, NextApiResponse} from "next";


// don't cache the results
export const revalidate = 0;

export type ConnectionDetails = {
  serverUrl: string;
  roomName: string;
  participantName: string;
  participantToken: string;
};

export default async function handler(
    _req: NextApiRequest,
    res: NextApiResponse
) {

    const {courseId} = _req.query ;

    try {
        const upstream = await fetch(`http://localhost:8087/api/books/${courseId}`, {
            cache: "no-store",
        });

        if (!upstream.ok) {
            res.status(502).json({ error: "Upstream error", status: upstream.status });
        }

        const data = await upstream.json();

        res.status(200).json({ data });
    } catch (err) {
        res.status(500).json({ error: "Fetch failed", details: (err as Error).message  });
    }
}

