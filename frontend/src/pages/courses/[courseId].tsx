import {Geist, Geist_Mono} from "next/font/google";
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

export default function Course() {

    const router = useRouter();
    const {courseId} = router.query;
    const [course, setCourse] = useState<any>();
    const [sectionVisibility, setSectionVisibility] = useState<any>({});


    useEffect(() => {

        const courseId = 1;

        fetch(`/api/courses/${courseId}`)
            .then((res) => res.json())
            .then((resJson) => {
                setCourse(resJson?.data);
            })
            .catch((err) => {
                setCourse(null);
            });

    }, []);

    return (
        <div
            className={`${geistSans.className} ${geistMono.className} font-sans flex mt-25 p-5`}
        >
            {!!course && <div>

                <b>{course.title}</b>

                {course.chapters?.map((chapter: any, index: number) => <div className={"mt-5"}>

                    {"___"}{index+1}. {chapter.title}

                    {chapter.subchapters?.map((subchapter: any, jindex: number) => <div>

                        {"______"}{index+1}.{jindex+1}. {subchapter.title}


                        {subchapter.sections?.map((section: any, kindex: number) => <div>

                            <div>{"_________"}{index+1}.{jindex+1}.{kindex+1}. <span className={"cursor-pointer underline text-blue-500"} onClick={() => setSectionVisibility({...sectionVisibility, [`${index}.${jindex}.${kindex}`]: !sectionVisibility[`${index}.${jindex}.${kindex}`]})}>{section.title}</span></div>
                            <div className={sectionVisibility[`${index}.${jindex}.${kindex}`] ? ``:`hidden`}>
                                {section.content}
                            </div>

                        </div>)
                        }

                    </div>)}

                    </div>)
                }

            </div>}
        </div>
    );
}


