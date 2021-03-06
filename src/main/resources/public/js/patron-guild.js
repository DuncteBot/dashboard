function submitForm(token) {
    let userId = id('user_id').value;
    let guildId = id('guild_id').value;

    id('btn').disabled = true;
    id('btn').classList.add('disabled');
    id('msg').innerHTML = 'Checking ids.....';

    fetch('/api/check/user-guild', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        },
        body: JSON.stringify({
            user_id: userId,
            guild_id: guildId,
            captcha_response: token
        })
    })
        .then((blob) => blob.json())
        .then((json) => {
            reset('');

            if (json.code !== 200) {
                id('confirm').innerHTML = `ERROR: <b>${getMessage(json.message)}</b>`;
                return;
            }

            id('confirm').innerHTML = `
                <div class="row">
                    <div class="col s12 m6">
                        <div class="card indigo">
                            <div class="card-content white-text">
                                <span class="card-title">Confirm your selection</span>
                                <p>To make sure that the patron perks get added to the correct user and server,
                                    please confirm your input</p>
                                <br>

                                <p>User: <i>${json.user.formatted}</i></p>
                                <p>Server: <i>${json.guild.name}</i></p>
                                <br>

                                <p>If this is not correct please change the ids in the form and press submit again.</p>
                            </div>
                            <div class="card-action ">
                                <a href="#" class="btn green white-text text-lighten-4" onclick="submitPatronForm('${json.token}'); return false;">This is correct</a>
                            </div>
                        </div>
                    </div>
                </div>
                    `;
        })
        .catch((e) => {
            reset(e.message);
            console.log(e);
            console.error(e)
        });
}

function submitPatronForm(token) {
    id('token').value = token;
    id('patrons').submit();
}

function reset(message) {
    window.scrollTo(0, 0);
    id('token').value = '';
    id('btn').disabled = false;
    id('btn').classList.remove('disabled');
    id('msg').innerHTML = message;
}
