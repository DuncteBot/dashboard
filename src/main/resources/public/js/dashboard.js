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
                let settingsLink = `<a href="https://discord.com/oauth2/authorize?client_id=210363111729790977&scope=bot&permissions=1609952470&guild_id=${guild.id}" target="_blank">Invite Bot</a>`;

                if (guild.members > -1) {
                    members = `${guild.members} members`;
                    settingsLink = `<a href="/server/${guild.id}/">Edit settings</a>`;
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
