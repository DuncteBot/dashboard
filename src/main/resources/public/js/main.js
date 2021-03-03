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

