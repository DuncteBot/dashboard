/*
 * MIT License
 *
 * Copyright (c) 2020 Duncan Sterken
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
