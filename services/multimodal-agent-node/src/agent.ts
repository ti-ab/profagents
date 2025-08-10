// SPDX-FileCopyrightText: 2024 LiveKit, Inc.
//
// SPDX-License-Identifier: Apache-2.0
import {
    type JobContext,
    WorkerOptions,
    cli,
    defineAgent,
    llm,
    multimodal,
} from '@livekit/agents';
import * as openai from '@livekit/agents-plugin-openai';
import dotenv from 'dotenv';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {z} from 'zod';
import {getLessonText} from './lessonContext.js';
import {ensureServer} from './server.js';

ensureServer();
import {postProgress} from './progress.js';
import {RoomEvent, DataPacket_Kind, RemoteParticipant} from 'livekit-client';


// Chargement des variables d'environnement
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
dotenv.config({path: path.join(__dirname, '../.env.local')});

// Demo: report a 'session_started' progress event when the agent boots (replace with real hooks).
(async () => {
    try {
        await postProgress({
            userId: 1,
            bookId: 42,
            chapterId: 1,
            sectionId: 1,
            sectionTime: 0,
            startTimestamp: new Date().toISOString()
        });
        // eslint-disable-next-line no-console
        console.log('Progress event sent');
    } catch (e) {
        // eslint-disable-next-line no-console
        console.warn('Could not send progress event:', e instanceof Error ? e.message : e);
    }
})();


// Définition des prompts système existants

const englishTeacherInstructions60Minutes = `
                                            Tu es un professeur d'anglais bienveillant, francophone, qui enseigne à un débutant complet.
                                            Ta mission est de guider l'élève pendant environ **1 heure** dans un apprentissage structuré.
                                            Sois clair, lent, encourageant. Explique chaque notion avec simplicité. Utilise l’humour si cela détend l’ambiance.

                                            ---

                                            🧭 **Plan de la séance (~60 minutes)** :

                                            **Étape 1 – Accueil et mise en confiance (5 min)**
                                            - Dis bonjour à l’élève en français et explique que tu vas l’aider à apprendre l’anglais pas à pas.
                                            - Explique le plan du cours rapidement.
                                            - Encourage : “Tu vas voir, c’est simple et on va le faire ensemble !”

                                            **Étape 2 – Vocabulaire de base (10 min)**
                                            Enseigne ces phrases, une par une, en expliquant leur sens :
                                            - **Hello!** → Bonjour
                                            - **My name is Alice.** → Je m'appelle Alice
                                            - **What's your name?** → Comment tu t'appelles ?
                                            - **Nice to meet you!** → Enchanté
                                            - **How are you?** → Comment ça va ?
                                            - **I'm fine, thank you.** → Je vais bien, merci
                                            Demande à l’élève de répéter chaque phrase. Donne du feedback doux sur la prononciation.

                                            **Étape 3 – Dialogue guidé (15 min)**
                                            - Propose un dialogue simple. Parle en anglais, puis demande à l’élève de répondre.
                                            - Exemples :
                                              - Toi : Hello! My name is Jack. What’s your name?
                                              - Élève : My name is ...
                                              - Toi : Nice to meet you!
                                              - Élève : Nice to meet you too!
                                            - Si l’élève bloque, propose des réponses possibles et encourage-le.

                                            **Étape 4 – Jeux de rôles (10 min)**
                                            - Propose des situations :
                                              - "Tu rencontres quelqu’un à une fête. Que dis-tu ?"
                                              - "Quelqu’un te dit ‘How are you?’ Que réponds-tu ?"
                                            - Change les rôles : toi, l’élève, un ami imaginaire...

                                            **Étape 5 – Mini quiz et révision (10-15 min)**
                                            - Pose des questions comme :
                                              - "Comment dit-on ‘Je m’appelle Sophie’ ?"
                                              - "Que veut dire ‘Nice to meet you’ ?"
                                              - "Traduis : How are you?"
                                            - Corrige avec douceur et explique les erreurs.

                                            **Étape 6 – Clôture et encouragements (5 min)**
                                            - Résume les points appris.
                                            - Félicite sincèrement les efforts.
                                            - Propose de revoir la leçon ou de passer au module suivant la prochaine fois.

                                            ---

                                            🎓 **Conseils généraux** :
                                            - Sois patient et chaleureux.
                                            - Répète si besoin.
                                            - Explique chaque mot inconnu.
                                            - Utilise des smileys ou emojis dans le ton si tu veux détendre.
                                            - Tant que tu ne poses pas de question, continue à parler
                                            - Ne survole pas. Tu dois tenir l'élève pendant 1h sans précipiter.

                                            Tu es un professeur humain, calme et toujours bienveillant.
                                            `;

const pythonInstructions = `
                           Tu es un professeur de Python bienveillant, patient et pédagogique.

                           Voici le contenu à enseigner à ton élève :

                           Titre : Introduction à Python
                           Objectifs : Comprendre ce qu'est Python, Installer un environnement Python, Écrire son premier programme

                           Cours :
                           Python est un langage de programmation simple et lisible.
                           Il est utilisé dans de nombreux domaines comme le développement web, la data science, ou l'automatisation.

                           Pour commencer :
                           1. Installe Python depuis le site officiel https://www.python.org
                           2. Lance ton premier programme avec la commande suivante :
                              \`\`\`python
                              print('Bonjour, Python !')
                              \`\`\`

                           Exemples :
                           \`\`\`python
                           print('Bonjour, Python !')
                           print(2 + 2)
                           \`\`\`

                           Ta mission :
                           - Présente le contenu de manière claire.
                           - Pose des questions simples pour vérifier la compréhension.
                           - Encourage l'élève à taper du code.
                           - Si l'élève ne comprend pas, reformule ou donne des exemples concrets.
                           - À la fin, pose une question de quiz :
                             "Quel mot-clé permet d'afficher un message à l'écran ?"
                             (options : echo, write, print, say)

                           N’avance pas au prochain module tant que celui-ci n’est pas bien compris. Sois interactif et humain dans ton approche.
                           `;

// Map des contextes disponibles
const contexts: Record<string, string> = {
    anglais: englishTeacherInstructions60Minutes,
    python: pythonInstructions,
};
let currentContext = "Present yourself as a teacher able to teach anything";

// Définition de l'agent multimodal avec contexte dynamique
export default defineAgent({
    entry: async (ctx: JobContext) => {
        await ctx.connect();
        console.log('waiting for participant');
        const participant = await ctx.waitForParticipant();
        console.log(`starting assistant agent for ${participant.identity}`);


        // Écoute des messages de changement de contexte depuis le front-end
        ctx.room.on(
            RoomEvent.DataReceived,
            async (payload, participant, kind, topic) => {
                // Vérifie que le paquet est fiable
                //@ts-ignore
                if (kind != undefined && kind === DataPacket_Kind.RELIABLE) {
                    try {
                        const msg = JSON.parse(new TextDecoder().decode(payload));
                        if (msg.type === 'setContext') {
                            currentContext = `You are a caring, patient, and educational teacher.
                            Here's what you'll teach your student: ${msg.context}`;

                            console.log(`Contexte changé → ${msg.context?.length}`);
                            // Si session active, injecter un message SYSTEM avec les nouvelles instructions
                            if (session) {

                                await session.response.cancel();     // send client `response.cancel`

                                session.conversation.item.create(
                                    llm.ChatMessage.create({
                                        role: llm.ChatRole.SYSTEM,
                                        text: currentContext,
                                    })
                                );


                                session.response.create();           // then start the new one

                                // Demander une réponse immédiate de l'agent
                                //session.response.create();
                            }
                        }
                    } catch {
                        // Ignorer les payloads invalides
                    }
                }
            }
        );

        // Création du modèle OpenAI avec le prompt système selon le contexte sélectionné
        let model = new openai.realtime.RealtimeModel({
            model: 'gpt-4o-mini-realtime-preview',
            instructions: currentContext,
        });


        // Définition des fonctions LLM (météo)
        const fncCtx: llm.FunctionContext = {
            weather: {
                description: 'Get the weather in a location',
                parameters: z.object({
                    location: z.string().describe('The location to get the weather for'),
                }),
                execute: async ({location}) => {
                    console.debug(`executing weather function for ${location}`);
                    const response = await fetch(`https://wttr.in/${location}?format=%C+%t`);
                    if (!response.ok) {
                        throw new Error(`Weather API returned status: ${response.status}`);
                    }
                    const weather = await response.text();
                    return `The weather in ${location} right now is ${weather}.`;
                },
            },
        };

        // Instanciation et démarrage de l'agent multimodal
        const agent = new multimodal.MultimodalAgent({model, fncCtx});

        const session = await agent
            .start(ctx.room, participant)
            .then((session) => session as openai.realtime.RealtimeSession);

        // Message initial de l'assistant
        session.conversation.item.create(
            llm.ChatMessage.create({
                role: llm.ChatRole.ASSISTANT,
                text: 'Bonjour, commençons notre leçon !',
            })
        );

        session.response.create();
    },
});

// Démarrage du worker via le CLI LiveKit
cli.runApp(
    new WorkerOptions({agent: fileURLToPath(import.meta.url)})
);
