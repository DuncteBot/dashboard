eventBus.once('loaded', () => {
    M.FormSelect.init(document.querySelectorAll('select'));
    M.updateTextFields();
    M.Range.init(document.querySelector('input[name="ai-sensitivity"]'));
});

eventBus.on('click', (event) => {
    const element = event.target;

    if (element.id === 'add_warn_action') {
        addWarnAction();
        return;
    }

    const data = element.dataset;

    if (data.removeAction) {
        removeWarnAction(data.removeAction);
    }
});
