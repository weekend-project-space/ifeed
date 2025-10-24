import MarkdownIt from 'markdown-it';

const markdown = new MarkdownIt({
    html: false,
    linkify: true,
    breaks: true
});

export function md2html(rawContent: String) {
    return markdown.render(rawContent)
}