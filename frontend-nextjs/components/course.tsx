'use client';

import React, { useState } from 'react';
import { Button } from '@/components/ui/button';
import {sleep} from "@/app/utils/js";

type Section = { id: number; idx?: number; title: string; content?: string };
type Subchapter = { id: number; idx?: number; title: string; sections: Section[] };
type Chapter = { id: number; idx?: number; title: string; subchapters: Subchapter[] };

interface CourseData {
    id: number;
    title: string;
    chapters: Chapter[];
}

interface CourseProps extends React.ComponentProps<'div'> {
    disabled: boolean;
    startButtonText: string;
    onStartCall: () => void;
    courseId: number;
    course?: CourseData | null;
}

export const Course: React.FC<CourseProps> = ({
                                                  disabled,
                                                  startButtonText,
                                                  onStartCall,
                                                  courseId,
                                                  course,
                                                  setCtxKey,
                                                  ...divProps
                                              }) => {
    const [selectedSectionId, setSelectedSectionId] = useState<number | null>(null);
    const [selectedPath, setSelectedPath] = useState<string | null>(null);
    const [selectedSectionContent, setSelectedSectionContent] = useState<string | null>(null);

    function handleSelectSection(
        section: Section,
        chapter: Chapter,
        subchapter: Subchapter,
    ) {
        setSelectedSectionId(section.id);
        setSelectedSectionContent(section.content);

        const chLabel =
            typeof chapter.idx === 'number' ? `Chapitre ${chapter.idx + 1}` : `Chapitre`;
        const scLabel =
            typeof subchapter.idx === 'number'
                ? `Sous-chapitre ${subchapter.idx + 1}`
                : `Sous-chapitre`;
        const sLabel =
            typeof section.idx === 'number' ? `Section ${section.idx + 1}` : `Section`;

        // Ex: "Chapitre 2 > Sous-chapitre 1 > Section 3 – Titre"
        setSelectedPath(
            `${chLabel} > ${scLabel} > ${sLabel} – ${section.title}`.trim(),
        );
    }

    async function startWithLesson() {
        try {
            if (!selectedSectionId) {
                alert("Sélectionne d’abord une section.");
                return;
            }


            setCtxKey(selectedSectionContent);

            /*await fetch('/api/agent/lesson', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ courseId, sectionId: selectedSectionId }),
            });*/
        } catch (e) {
            // On journalise mais on n’empêche pas l’appel de démarrer
            // eslint-disable-next-line no-console
            console.warn('Could not push lesson to agent', e);
        } finally {
            onStartCall();
        }
    }

    return (
        <div {...divProps} className={`mt-24 space-y-4 ${divProps.className ?? ''}`}>
            <div className="flex items-center justify-between gap-3">
                <div className="min-w-0">
                    <h2 className="text-xl font-semibold truncate">
                        {course?.title ?? 'Cours'}
                    </h2>
                    {selectedPath ? (
                        <p className="text-sm text-muted-foreground mt-1">
                            Section sélectionnée : <span className="font-medium">{selectedPath}</span>
                        </p>
                    ) : (
                        <p className="text-sm text-muted-foreground mt-1">
                            Sélectionne une section pour démarrer l’appel.
                        </p>
                    )}
                </div>

                <Button
                    disabled={disabled}
                    onClick={startWithLesson}
                    className="shrink-0"
                    aria-label="Start Call"
                >
                    {startButtonText}
                </Button>
            </div>

            {/* Arbre du cours */}
            {course && course.chapters?.length ? (
                <div className="space-y-4">
                    {course.chapters.map((chapter, chapterIndex) => (
                        <div key={chapter.id} className="rounded-xl border p-4">
                            <h3 className="text-lg font-semibold">
                                {typeof chapter.idx === 'number'
                                    ? `Chapitre ${chapter.idx + 1}. `
                                    : ''}
                                {chapter.title}
                            </h3>

                            <div className="mt-3 space-y-3">
                                {chapter.subchapters?.map((subchapter, subchapterIndex) => (
                                    <div key={subchapter.id} className="rounded-lg bg-muted/40 p-3">
                                        <h4 className="font-medium">
                                            {typeof subchapter.idx === 'number'
                                                ? `Sous-chapitre ${subchapter.idx + 1}. `
                                                : ''}
                                            {subchapter.title}
                                        </h4>

                                        <ul className="mt-2 space-y-1">
                                            {subchapter.sections?.map((section, sectionIndex) => {
                                                const isSelected = selectedSectionId === section.id;
                                                return (
                                                    <li
                                                        key={section.id}
                                                        className="flex items-center justify-between gap-2 rounded-lg px-2 py-1 hover:bg-muted transition-colors"
                                                    >
                                                        <div className="min-w-0">
                                                            <h5 className="truncate">
                                                                {typeof section.idx === 'number'
                                                                    ? `- ${section.idx + 1}. `
                                                                    : '- '}
                                                                {section.title}
                                                            </h5>
                                                        </div>

                                                        <Button
                                                            variant={isSelected ? 'secondary' : 'outline'}
                                                            size="sm"
                                                            onClick={() =>
                                                                handleSelectSection(section, chapter, subchapter)
                                                            }
                                                            aria-pressed={isSelected}
                                                        >
                                                            {isSelected ? 'Sélectionnée' : 'Sélectionner'}
                                                        </Button>
                                                    </li>
                                                );
                                            })}
                                        </ul>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ))}
                </div>
            ) : (
                <div>Loading course...</div>
            )}
        </div>
    );
};
