import { marked } from 'marked'

export function md2html(rawContent: String) {
    const r = marked(rawContent)
    console.log(r)
    return r
}