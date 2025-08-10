
let _lessonText: string | null = null;

export function setLessonText(text: string) {
  _lessonText = text;
}

export function getLessonText(): string | null {
  return _lessonText;
}
