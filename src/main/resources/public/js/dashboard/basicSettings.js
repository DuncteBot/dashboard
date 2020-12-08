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

function displayColor(value) {
    const preview = id('embedPreview');

    preview.style.background = value;

    if (brightnessByColor(value) >= 150) {
        preview.classList.add('black-text');
        preview.classList.remove('white-text');
    } else {
        preview.classList.remove('black-text');
        preview.classList.add('white-text');
    }
}

displayColor('$guildColor');

function showColorPicker() {
    id('embedColor').click()
}


// Adapted from https://gist.github.com/w3core/e3d9b5b6d69a3ba8671cc84714cca8a4
function brightnessByColor (color) {
    const match = color.substr(1).match(color.length === 7 ? /(\S{2})/g : /(\S)/g);

    if (match) {
        const r = parseInt(match[0], 16);
        const g = parseInt(match[1], 16);
        const b = parseInt(match[2], 16);

        return (
            (r * 299) +
            (g * 587) +
            (b * 114)
        ) / 1000;
    }

    return null;
}

/**
 *
 * @param {string} selectId
 * @param {Array<string>|string} checkBox
 */
function checkSelect(selectId, checkBox) {
    let boxes;

    if (Array.isArray(checkBox)) {
        boxes = checkBox.map(id);
    } else {
        boxes = id(checkBox);
    }

    let select = id(selectId);

    if (select.value) {
        boxes.forEach((box) => {
            box.checked = true;
            box.disabled = false;
        });
    } else {
        boxes.forEach((box) => {
            box.checked = false;
            box.disabled = true;
        });
    }
}
