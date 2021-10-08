<template id="base-settings">
    <div>
        <app-menu :guild-name="settingData.loaded ? guild.name : null"></app-menu>

        <div class="container">
            Settings
            <div v-if="settingData.loaded">
                {{ settings }}
                <form action="#" class="row" onsubmit="return false;">
                    <app-settings-basic :settings="settings" :roles="roles"/>
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

            return {
                settingData: new LoadableData(`/api/guilds/${guildId}/settings`, false),
            };
        },
        watch: {
            // TODO: ugly
            'settingData.loaded' () {
                setTimeout(() => {
                    M.FormSelect.init(document.querySelectorAll('select'));
                    M.updateTextFields();
                    M.Range.init(document.querySelector('input[name="ai-sensitivity"]'));
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
