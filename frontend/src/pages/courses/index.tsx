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

        const courseId = 1;

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
      className={`${geistSans.className} ${geistMono.className} font-sans grid grid-rows-[20px_1fr_20px] items-center justify-items-center min-h-screen p-8 pb-20 gap-16 sm:p-20`}
    >
        {courses?.map((course: any) => <div className={"text-blue-500 cursor-pointer underline"} onClick={() => router.push(`/courses/${course.id}`)}>
            {course.title}
        </div>)}
    </div>
  );
}
