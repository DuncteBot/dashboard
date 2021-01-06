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

window.eventBus = new EventEmitter();

// We had to rename this form _ to id because
// the fucking patreon button has lodash
function id(el) {
    return document.getElementById(el);
}

function hide(itemId) {
    id(itemId).style.display = 'none';
}

function unHide(itemId) {
    id(itemId).style.display = 'block';
}

document.addEventListener('DOMContentLoaded', () => {
    id('year').innerHTML = `${(new Date()).getFullYear()}`;
    M.Sidenav.init(document.querySelectorAll('.sidenav'), {
        onOpenEnd: () => {
            window.navOpen = true;
        },
        onCloseEnd: () => {
            window.navOpen = false;
        },
    });

    // M.AutoInit();

    eventBus.emit('loaded');
});

document.addEventListener('click', (event) => eventBus.emit('click', event));

function getMessage(m) {
    switch (m) {
        case 'missing_input':
            return 'Please fill in all fields';
        case 'no_user':
            return 'The specified user id did not resolve any users.';
        case 'no_guild':
            return 'The specified server id did not resolve any servers.';
        default:
            return m;
    }
}

