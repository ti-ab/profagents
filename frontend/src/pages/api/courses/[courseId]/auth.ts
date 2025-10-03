import {NextResponse, NextRequest} from 'next/server';
import {getServerSession} from "next-auth/next";
import {authOptions} from "../../auth/[...nextauth]/route";
import { getToken } from "next-auth/jwt";
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

    const session = await getServerSession(authOptions);

    if (!session) {
        res.status(401).json({ error: "Unauthorized" });
    }

    // Si ton backend nécessite le token Keycloak, on le récupère et on le propage
    const token = await getToken({ req: _req }); // nécessite NEXTAUTH_SECRET
    const headers: HeadersInit = {
        "Cache-Control": "no-store",
        ...(token ? { Authorization: `Bearer ${token.accessToken ?? token}` } : {}),
    };

    try {
        const upstream = await fetch(`http://nginx/api/books/${courseId}`, {
            headers,
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

