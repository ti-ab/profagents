'use client';


import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Room, RoomEvent, DataPacket_Kind } from 'livekit-client';
import { motion } from 'motion/react';
import { RoomAudioRenderer, RoomContext, StartAudio } from '@livekit/components-react';
import { toastAlert } from '@/components/alert-toast';
import { SessionView } from '@/components/session-view';
import { Toaster } from '@/components/ui/sonner';
import useConnectionDetails from '@/hooks/useConnectionDetails';
import type { AppConfig } from '@/lib/types';
import {Course} from "@/components/course";

const MotionWelcome = motion.create(Course);
const MotionSessionView = motion.create(SessionView);


interface AppProps {
    appConfig: AppConfig;
    courseId: number;
}

export default function CoursePage({ appConfig, courseId }: AppProps) {
    const room = useMemo(() => new Room(), []);
    const [sessionStarted, setSessionStarted] = useState(false);
    const [course, setCourse] = useState(null);
    const { connectionDetails, refreshConnectionDetails } = useConnectionDetails();


    const [ctxKey, setCtxKey] = useState<'anglais' | 'python'>('anglais');

    const router = useRouter();

    // Exemple d’appel client-side, dans un useEffect par exemple
    useEffect(() => {
        fetch(`/api/courses/${courseId}`)
            .then((res) => res.json())
            .then((data) => {
                setCourse(data);
                window.scrollTo(0, 0);
            })
            .catch((err) => {
                setCourse(null);
            });

        setTimeout(() => window.scrollTo(0, 0), 500);
    }, [courseId]);


    useEffect(() => {
        const onDisconnected = () => {
            setSessionStarted(false);
            refreshConnectionDetails();
        };
        const onMediaDevicesError = (error: Error) => {
            toastAlert({
                title: 'Encountered an error with your media devices',
                description: `${error.name}: ${error.message}`,
            });
        };
        room.on(RoomEvent.MediaDevicesError, onMediaDevicesError);
        room.on(RoomEvent.Disconnected, onDisconnected);
        return () => {
            room.off(RoomEvent.Disconnected, onDisconnected);
            room.off(RoomEvent.MediaDevicesError, onMediaDevicesError);
        };
    }, [room, refreshConnectionDetails]);

    useEffect(() => {
        let aborted = false;
        if (sessionStarted && room.state === 'disconnected' && connectionDetails) {
            (async () => {
                    await Promise.all([
                        room.localParticipant.setMicrophoneEnabled(true, undefined, {
                            preConnectBuffer: appConfig.isPreConnectBufferEnabled,
                        }),
                        room.connect(connectionDetails.serverUrl, connectionDetails.participantToken),
                    ]).catch((error) => {
                        if (aborted) {
                            // Once the effect has cleaned up after itself, drop any errors
                            //
                            // These errors are likely caused by this effect rerunning rapidly,
                            // resulting in a previous run `disconnect` running in parallel with
                            // a current run `connect`
                            return;
                        }

                        toastAlert({
                            title: 'There was an error connecting to the agent',
                            description: `${error.name}: ${error.message}`,
                        });
                    });

                    await sleep(3000);

                    sendContext(ctxKey);
                }
            )();
        }
        return () => {
            aborted = true;
            room.disconnect();
        };
    }, [room, sessionStarted, connectionDetails, appConfig.isPreConnectBufferEnabled]);


    function sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    const sendContext = (key: string) => {
        const payload = JSON.stringify({ type: 'setContext', context: key });
        room.localParticipant.publishData(
            new TextEncoder().encode(payload),
            DataPacket_Kind.RELIABLE,
        );
    };

    const { startButtonText } = appConfig;

    return (
        <>


            <MotionWelcome courseId={courseId}
                key="course"
                startButtonText={startButtonText}
                course={course}
                onStartCall={() => setSessionStarted(true)}
                disabled={sessionStarted}
                initial={{ opacity: 0 }}
                animate={{ opacity: sessionStarted ? 0 : 1 }}
                transition={{ duration: 0.5, ease: 'linear', delay: sessionStarted ? 0 : 0.5 }}
                router={router}
                setCtxKey={setCtxKey}
            />


            <RoomContext.Provider value={room}>
                <RoomAudioRenderer />
                <StartAudio label="Start Audio" />
                {/* --- */}
                <MotionSessionView
                    key="session-view"
                    appConfig={appConfig}
                    disabled={!sessionStarted}
                    sessionStarted={sessionStarted}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: sessionStarted ? 1 : 0 }}
                    transition={{
                        duration: 0.5,
                        ease: 'linear',
                        delay: sessionStarted ? 0.5 : 0,
                    }}
                />
            </RoomContext.Provider>

            <select
                value={ctxKey}
                onChange={e => {
                    setCtxKey(e.target.value);
                }}
            >
                <option value="anglais">Leçon d’anglais</option>
                <option value="python">Leçon de Python</option>
            </select>

            <Toaster />
        </>
    );
}
