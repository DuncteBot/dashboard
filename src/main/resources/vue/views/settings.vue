<template id="base-settings">
    <div>
        <!-- Save button-->
        <div class="fixed-action-btn">
            <a @click.prevent="saveSettings()"
                class="btn-floating btn-large waves-effect waves-light waves-ripple blue accent-4 white">
                <i class="large material-icons">save</i>
            </a>
        </div>

        <app-menu :guild-name="settingData.loaded ? guild.name : null"
                  :showing="showingItem"
                  @change-menu="setShow($event)"
        ></app-menu>

        <div class="container">
            Settings
            <div v-if="settingData.loaded">
                {{ originalSettings }}
                {{ settings }}
                <form action="#" class="row" onsubmit="return false;">
                    <app-settings-basic
                        v-show="showingItem === 'basic'"
                        :settings="settings"
                        :roles="roles"/>
                </form>
            </div>
        </div>
    </div>
</template>

<script>
    Vue.component('settings', {
        template: '#base-settings',
        data () {
            const guildId = this.$javalin.pathParams['guildId'];
            const settingsURL = `/api/guilds/${guildId}/settings`;

            return {
                originalSettings: {}, // unsaved changes checking
                saving: false,
                settingsURL,
                settingData: new LoadableData(settingsURL, false),
                showingItem: (window.location.hash || 'basic').replace('#', ''),
            };
        },
        watch: {
            // TODO: ugly
            'settingData.loaded' () {
                setTimeout(() => {
                    this.originalSettings = { ...this.settings };
                    M.FormSelect.init(document.querySelectorAll('select'));
                    M.updateTextFields();
                    M.Range.init(document.querySelector('input[name="ai-sensitivity"]'));
                }, 0);
            },
            show () {
                setTimeout(() => {
                    window.scrollTo(0, 0);

                    document.querySelectorAll('textarea').forEach((it) => {
                        M.textareaAutoResize(it);
                    });
                }, 0);
            },
        },
        computed: {
            settings () {
                return this.settingData.data.settings;
            },
            roles () {
                return this.settingData.data.roles;
            },
            channels () {
                return this.settingData.data.channels;
            },
            guild () {
                return this.settingData.data.guild;
            },
        },
        methods: {
            async saveSettings () {
                if (this.saving) {
                    return;
                }

                this.saving = true;

                toast('Saving');

                const res = await fetch(this.settingsURL, {
                    method: 'POST',
                    credentials: 'include',
                    body: JSON.stringify(this.settings)
                });
                const data = await res.json();

                console.log(data);

                toast('Saved!');
            },
            setShow (item) {
                this.showingItem = item;
                window.location.hash = item;
            },
        },
    });
</script>

<style>
    .caret {
        fill: #FFFFFF !important;
    }

    select {
        display: none;
    }
</style>
