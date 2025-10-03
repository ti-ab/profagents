import {Geist, Geist_Mono} from "next/font/google";
import * as React from "react";
import {Button, TextField} from "@mui/material";

const geistSans = Geist({
    variable: "--font-geist-sans",
    subsets: ["latin"],
});

const geistMono = Geist_Mono({
    variable: "--font-geist-mono",
    subsets: ["latin"],
});

export default function Home() {

    return (
        <div
            className={`${geistSans.className} ${geistMono.className} font-sans items-center items-center justify-items-center min-h-screen sm:p-20`}
        >
            <div className={"mt-5"}>
                <TextField fullWidth={true} id="outlined-basic" label="Course title" variant="outlined"/>
            </div>

            <div className={"p-4"}>
                <Button fullWidth={true} variant="contained">Generate</Button>
            </div>
        </div>
    );
}
