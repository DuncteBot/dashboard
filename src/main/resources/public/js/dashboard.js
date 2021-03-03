eventBus.once('loaded', () => {
    fetch('/api/user-guilds', {
        credentials: 'same-origin'
    })
        .then(response => response.json())
        .then(json => {
            const div = id('guilds');

            if (!json.success) {
                div.innerHTML = `<h1 class="center">Session not valid</h1>
                              <h5 class="center">Please refresh your browser or <a href="/logout">click here</a> to log out</h5>`;

                return;
            }

            if (json.guilds.length < 0) {
                div.innerHTML = `<h1 class="center">No servers found</h1>
                              <h5 class="center">Make sure that you have administrator permission in at least 1 server</h5>`;

                return;
            }

            div.innerHTML = '';

            for (const guild of json.guilds) {
                let members = 'Bot not in server';
                let settingsLink = `<a href="https://r.duncte.bot/inv&guild_id=${guild.id}" target="_blank">Invite Bot</a>`;

                if (guild.members > -1) {
                    members = `${guild.members} members`;
                    settingsLink = `<a href="/server/${guild.id}">Edit settings</a>`;
                }

                div.innerHTML += `<div class="col s12 m6 l4 xl3">
                            <div class="card horizontal hoverable">
                                <div class="card-image">
                                    <img src="${guild.iconUrl}?size=256">
                                </div>
                                <div class="card-stacked">
                                    <div class="card-content">
                                        <h6 class="truncate">${guild.name}</h6>
                                        <p>${members}</p>
                                    </div>
                                    <div class="card-action">
                                        ${settingsLink}
                                    </div>
                                </div>
                            </div>
                        </div>`;
            }

            div.innerHTML += `<div class="col s12 m6 l4 xl3">
                            <div class="card horizontal hoverable">
                                <div class="card-image">
                                    <img src="https://cdn.discordapp.com/embed/avatars/${Math.floor(Math.random() * 5)}.png?size=256" />
                                </div>
                                <div class="card-stacked">
                                    <div class="card-content">
                                        <h6 class="truncate">Your total server count:</h6>
                                        <p>${json.total} Servers</p>
                                    </div>
                                    <div class="card-action">
                                        <a href="https://patreon.com/DuncteBot" target="_blank">Become a patron</a>
                                    </div>
                                </div>
                            </div>
                        </div>`;

        })
        .catch(() =>
            id('guilds').innerHTML = 'Your session has expired, please refresh your browser'
        );
});
