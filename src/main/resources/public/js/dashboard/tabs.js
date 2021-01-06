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

const ul = document.getElementById('server-setting-tabs');
const tabs = document.querySelector('.db-menus').children;

ul.addEventListener('click', (e) => {
    if (e.target.nodeName !== 'A') {
        return;
    }

    setActive(e.target.getAttribute('href'));
});

if (window.location.hash) {
    setActive(window.location.hash);
}

function setActive(item) {
    // skip that one url
    if (item[0] === '/') {
        return;
    }

    const targetTab = document.querySelector(item);

    if (!targetTab) {
        return;
    }

    for (const li of ul.children) {
        // since we are looping over all LI elements we need to check if it contains the a-tag
        if (li.classList.contains('divider')) {
            continue;
        }

        if (li.firstElementChild.getAttribute('href') === item) {
            li.classList.add('active');
        } else {
            li.classList.remove('active');
        }
    }

    for (const tab of tabs) {
        tab.classList.remove('active');
    }

    targetTab.classList.add('active');

    const nav = M.Sidenav.getInstance(document.querySelector('.sidenav'));

    // close the sidenav if it's open
    if (nav && window.navOpen) {
        nav.close();
    }

    setTimeout(() => {
        window.scrollTo(0, 0);

        document.querySelectorAll('textarea').forEach((it) => {
            M.textareaAutoResize(it);
        });
    }, 10);
}
