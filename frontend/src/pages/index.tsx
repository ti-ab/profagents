import {Geist, Geist_Mono} from "next/font/google";
import * as React from "react";
import {Button, TextField} from "@mui/material";
import {useCallback, useState} from "react";

const geistSans = Geist({
    variable: "--font-geist-sans",
    subsets: ["latin"],
});

const geistMono = Geist_Mono({
    variable: "--font-geist-mono",
    subsets: ["latin"],
});

export default function Home() {


    const [title, setTitle] = useState<string>("");
    const [status, setStatus] = useState<string>();

    const generateCourse = useCallback(() => {

        setStatus("Generating...");

        fetch(`/api/courses/generate?bookDescription=${encodeURIComponent(title)}`)
            .then((res) => res.json())
            .then((resJson) => {
                setStatus(resJson?.data);
            })
            .catch((err) => {
                setStatus("Error during generation");
            });


    }, [title])

    return (
        <div
            className={`${geistSans.className} ${geistMono.className} font-sans items-center items-center justify-items-center min-h-screen sm:p-20`}
        >
            <div className={"mt-5"}>
                <TextField fullWidth={true} id="outlined-basic" label="Course title" variant="outlined" placeholder={"Préparation au language Java pour la certification Oracle OCA"} value={title} onChange={e => setTitle(e.target.value)}/>
            </div>

            <div className={"p-4"}>
                <Button fullWidth={true} variant="contained" onClick={generateCourse}>Generate</Button>
            </div>

            <div className={"h4 p-5"}>{status}</div>
        </div>
    );
}
