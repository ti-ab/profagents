import Image from "next/image";
import { Geist, Geist_Mono } from "next/font/google";
import {useEffect, useState} from "react";
import {useRouter} from "next/router";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export default function Home() {

    const router = useRouter();
    const [courses, setCourses] = useState<any>();


    useEffect(() => {

        fetch(`/api/courses`)
            .then((res) => res.json())
            .then((resJson) => {
                setCourses(resJson?.data);
            })
            .catch((err) => {
                setCourses(null);
            });

    }, []);

  return (
    <div
      className={`${geistSans.className} ${geistMono.className} font-sans h-screen mt-24 p-5`}
    >
        {courses?.map((course: any) => <div className={"text-blue-500 cursor-pointer underline p-2"} onClick={() => router.push(`/courses/${course.id}`)}>
            {course.title}
        </div>)}
    </div>
  );
}
