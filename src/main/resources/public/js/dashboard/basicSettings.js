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
