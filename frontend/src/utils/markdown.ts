import { marked } from 'marked'

export function md2html(rawContent: String) {
    return marked(rawContent)
}