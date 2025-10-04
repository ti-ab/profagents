import type {NextApiRequest, NextApiResponse} from "next";

// NOTE: you are expected to define the following environment variables in `.env.local`:
const API_KEY = process.env.LIVEKIT_API_KEY;
const API_SECRET = process.env.LIVEKIT_API_SECRET;
const LIVEKIT_URL = process.env.LIVEKIT_URL;

// don't cache the results
export const revalidate = 0;

export type ConnectionDetails = {
    serverUrl: string;
    roomName: string;
    participantName: string;
    participantToken: string;
};



export default async function handler(
    req: NextApiRequest,
    res: NextApiResponse,
) {

    const {bookDescription} = req.query;

    if (!!bookDescription) {

        const response = await fetch(
            `http://localhost:8087/api/generate?bookDescription=${encodeURIComponent(bookDescription + "")}`
        );


        const jsonResponse = await response.json();

        res.status(200).json({data: jsonResponse});
    } else {

        res.status(200).json({data: "Please enter a title"});
    }

}
