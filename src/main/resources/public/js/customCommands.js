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

function initModal() {
    window.storedCommands = {};
    window.editorRow = id('editorRow');
}

function showEditor() {
    editorRow.style.display = 'block';
    editorRow.scrollIntoView({behavior: 'smooth'});
}

function hideEditor() {
    editorRow.style.display = 'none';
}

function initEitor() {
    const el = id('editor');
    window.editor = CodeMirror.fromTextArea(el, {
        mode: 'jagtag',
        lineNumbers: true,
        indentWithTabs: false,
        styleActiveLine: true,
        matchBrackets: true,
        smartIndent: true,
        autoCloseBrackets: false,
        theme: 'monokai',
        electricChars: true,
        lineWrapping: true,
        hintOptions: {
            words: window.wordList
        },
        tabMode: 'indent'
    });

    window.editor.on('inputRead', function (editor, change) {
        if (change.text[0] === '{') {
            editor.showHint();
        }
        editor.save();
        id("chars").innerHTML = editor.getValue().length;
    });
}

function loadCommands() {
    fetch(`/api/custom-commands/${guildId}`, {
        credentials: "same-origin"
    })
        .then((response) => response.json())
        .then((json) => {

            const div = id("commands");

            if (!json.success) {
                div.innerHTML = `<h1 class="center">Session not valid</h1>
                              <h5 class="center">Please refresh your browser</h5>`;
                return;
            }

            if (!json.commands.length) {
                div.innerHTML = '<h1 class="center">No custom commands have been created yet</h1>';
                return;
            }

            div.innerHTML = "";

            for (const command of json.commands) {
                storedCommands[command.invoke] = command;

                div.innerHTML += `
                    <li class="collection-item">
                        <h6 class="left">${command.invoke}</h6>

                        <div class="right">
                            <a href="#" onclick="showCommand('${command.invoke}'); return false;"
                                class="waves-effect waves-light btn valign-wrapper"><i class="left material-icons">create</i> Edit</a>
                            <a href="#" onclick="deleteCommand('${command.invoke}'); return false;" 
                                class="waves-effect waves-light red btn valign-wrapper"><i class="left material-icons">delete</i> Delete</a>
                        </div>

                        <div class="clearfix"></div>
                    </li>`;
            }

        })
        .catch(
            () => id("commands").innerHTML = "Your session has expired, please refresh your browser"
        );
}

function showCommand(name) {
    const command = storedCommands[name];

    showModal(name, command.message, `saveEdit("${name}")`, command.autoresponse);
}

function deleteCommand(name) {
    const conf = confirm("Are you sure that you want to delete this command?");

    if (!conf) {
        return;
    }

    toast(`Deleting "${name}"!`);

    doFetch('DELETE', {invoke: name}, () => {
        toast("Deleted!");
        hideEditor();
        id("chars").innerHTML = 0;
        setTimeout(() => window.location.reload(), 500);
    });
}

function clearEditor() {
    id("chars").innerHTML = 0;
    editor.setValue("");
    editor.save();
    editor.refresh();
    id("commandName").value = '';
    hideEditor();
}

function saveEdit(name) {
    if (!name || !storedCommands[name]) {
        toast("Stop touching me");
        return;
    }

    toast("Saving...");

    const command = storedCommands[name];
    command.message = editor.getValue();
    command.autoresponse = id("autoresponse").checked;

    doFetch('PATCH', command, () => {
        toast("Saved!");
        hideEditor();
        id("chars").innerHTML = 0;
    });
}

function showModal(invoke, message, method, autoresponse) {
    editor.setValue(message);
    editor.save();
    id("commandName").value = invoke;
    id("autoresponse").checked = autoresponse;

    id("saveBtn").setAttribute("href", `javascript:${method};`);
    id("chars").innerHTML = message.length;

    showEditor();
    editor.refresh();
}

function prepareCreateNew() {
    id("chars").innerHTML = 0;
    id("commandName").value = '';
    editor.save();
    showModal("", "", "createNew()", false);
}

function createNew() {
    let name = id("commandName").value;
    name = name.replace(/\s+/g, '');

    if (name === "") {
        toast("Please give a name");
        return
    }

    if (name.length > 25) {
        toast("Name must be less than 25 characters");
        return
    }

    const action = editor.getValue();

    if (action === "") {
        toast("Message cannot be empty");
        return
    }

    if (action.length > 4000) {
        toast("Message cannot greater than 4000 characters");
        return
    }

    const command = {
        invoke: name,
        message: action,
        guildId: guildId,
        autoresponse: id("autoresponse").checked,
    };

    storedCommands[name] = command;

    toast("Adding command....");

    doFetch('POST', command, () => {
        toast("Command added");
        setTimeout(() => window.location.reload(), 500);
        // modal.close();
        id("chars").innerHTML = 0;
    });
}

function doFetch(method, body, cb) {
    fetch(`/api/custom-commands/${guildId}`, {
        method: method,
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(body)
    })
        .then((response) => response.json())
        .then((json) => {
            if (json.success) {
                cb(json);
                return
            }

            notSaveToast(json.message);
        })
        .catch((e) => {
            notSaveToast(e);
        });
}

function notSaveToast(m) {
    toast(`Could not save: ${m}`);
}

function toast(message) {
    M.toast({
        html: message,
    });
}
