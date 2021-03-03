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
