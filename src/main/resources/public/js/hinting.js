CodeMirror.registerHelper('hint', 'jagtag', (editor, options) => {
    const words = options.words;
    const cur = editor.getCursor();
    const token = editor.getTokenAt(cur);
    const to = CodeMirror.Pos(cur.line, token.end);
    let from = to;
    let term = "";

    if (token.string && /\w/.test(token.string[token.string.length - 1])) {
        term = token.string;
        from = CodeMirror.Pos(cur.line, token.start);
    }

    const found = [];
    for (const word of words) {
        if (word.text.slice(0, term.length) === term) {
            found.push(word);
        }
    }

    if (found.length) {
        return {
            list: found,
            from: from,
            to: to
        };
    }
});

const brace = 'builtin';
const semiColon = 'bracket';
CodeMirror.defineSimpleMode('jagtag', {
    start: [
        { regex: /\d/, token: 'number' },
        { regex: /\{/, token: brace, push: 'innerTag', indent: true },
        { regex: /[:}]/, token: brace },
        { regex: /[^\{}]*/, token: 'bracket' },
    ],
    comment: [
    ],
    innerTag: [
        { regex: /\{/, token: brace, push: 'innerTag', indent: true },
        { regex: /\}/, token: brace, pop: true, dedent: true },
        { regex: /([^{}:\| ]+)/, token: 'keyword' },

        { regex: /(:|\|+)(\{)/, token: [semiColon, brace], push: 'innerTag', indent: true },
        { regex: /(:|\|+)(})/, token: [semiColon, brace], pop: true, dedent: true },
        { regex: /(:|\|+)$/, token: [semiColon] },

        { regex: /([:|]+)(\d*)?([^{}:\|]*)?/, token: [semiColon, 'number', 'string'] },
        { regex: /^(\d*)?([^{}:\|]*)?/, token: ['number', 'string'] }
    ],
    args: [
        { regex: /\{/, token: brace, push: 'innerTag', indent: true },
        { regex: /\}/, token: brace, pop: true, dedent: true },
        { regex: /:/, token: semiColon },
        { regex: /\|/, token: semiColon },
        { regex: /[^{}:\|]+/, token: 'string' }
    ],
    meta: {}
});
