const actions = id('actions');

const getLiId = (id) => `warningAction${id}-li`;
const mapActionTypes = (selectedType) => warnActionTypes.map(
    ({id, name}) => `<option value="${id}" ${selectedType === id ? 'selected' : ''}>${name}</option>`
);

eventBus.once('loaded', () => {
    for (const action of warnActions) {
        const li = buildLi(action);

        actions.appendChild(li);
    }

    // Keep the button there for non patrons
    if (actions.children.length >= maxActions && guildPatron) {
        hide('add_warn_action');
    }
});

function buildLi(warnAction) {
    const li = document.createElement('li');
    const size = actions.children.length + 1;

    li.id = getLiId(size);
    li.classList.add('row');

    li.innerHTML = buildTemplate(warnAction, size);

    return li;
}

function buildTemplate(warnAction, num) {
    warnAction = warnAction || {
        type: {id: null, temp: false},
        threshold: 3,
        duration: 5
    };

    return `
        <div class="col s3">
            <div class="input-field">
                <select id="warningAction${num}"
                        name="warningAction${num}"
                        onchange="checkTempDuration(this)">
                    ${mapActionTypes(warnAction.type.id)}
                </select>
                <label for="warningAction${num}">Warning action</label>
            </div>
        </div>
        
        <div class="col s3" style="display: ${warnAction.type.temp ? 'block' : 'none'}">
            <div class="input-field">
                <input type="number" id="tempDays${num}"
                       name="tempDays${num}"
                       value="${warnAction.duration}"/>
                <label for="tempDays${num}">Duration</label>
            </div>
        </div>
        
        <div class="col s3">
            <div class="input-field">
                <input type="number" id="threshold${num}"
                       name="threshold${num}"
                       value="${warnAction.threshold}"/>
                <label for="threshold${num}">Warning threshold</label>
            </div>
        </div>
        
        <div class="col s1">
            <button type="button" class="btn red"
                    data-remove-action="${num}">
                        <i class="material-icons">delete_forever</i>
                    </button>
        </div>
    `;
}
