<template id="settings-basic">
    <div class="row">
        <div class="col s12">
            <section class="row section">
                <div class="input-field col s12 m1">
                    <input placeholder="db!" id="prefix" type="text" maxlength="10"
                           v-model="settings.prefix" required>
                    <label for="prefix">Prefix</label>
                </div>

                <div class="input-field col s12 m4">
                    <select id="autoRoleRole" v-model="settings.autorole">
                        <option value="" selected disabled>Select a role</option>
                        <option v-for="role in roles"
                                :key="role.id"
                                :value="role.id">@{{ role.name }}</option>
                        <option value="">Disable</option>
                    </select>
                    <label for="autoRoleRole">AutoRole</label>
                </div>
            </section>

            <section class="row">
                <div class="input-field col s12 m5">
                    <div class="switch">
                        Announce tracks: <br/>
                        <label>
                            Disabled
                            <input type="checkbox" v-model="settings.announceNextTrack"/>
                            <span class="lever"></span>
                            Enabled
                        </label>
                    </div>

                    <br/>

                    <div class="switch">
                        Stop command behavior:<br/>
                        <label>
                            Default behavior
                            <input type="checkbox" v-model="settings.allowAllToStop">
                            <span class="lever"></span>
                            Allow all to stop
                        </label>
                    </div>
                </div>

                <div class="input-field col s12 m5">
                    <button type="button" @click="showColorPicker()"
                            :style="{
                                backgroundColor: embedColor,
                            }"
                            :class="[
                                clsName,
                            ]"
                            class="btn-large waves-effect waves-light waves-ripple">
                        Embed color
                    </button>

                    <input type="color"
                           ref="color"
                           v-model="embedColor"/>
                </div>
            </section>

            <section class="row">
                <div class="divider"></div>
            </section>
        </div>
    </div>
</template>

<script>
    Vue.component('app-settings-basic', {
        template: '#settings-basic',
        props: {
            settings: {
                type: Object,
                required: true
            },
            roles: {
                type: Object,
                required: true
            },
        },
        data: () => ({
            clsName: 'white-text',
        }),
        watch: {
            embedColor () {
                if (this.brightnessByColor(this.embedColor) >= 150) {
                    this.clsName = 'black-text';
                } else {
                    this.clsName = 'white-text';
                }
            },
        },
        computed: {
            embedColor: {
                get () {
                    const hex = this.settings.embed_setting.embed_color;
                    return `#${hex.toString(16).padStart(6, '0')}`
                },
                set (value) {
                    this.settings
                        .embed_setting
                        .embed_color = parseInt(value.replace('#', ''), 16);
                },
            },
        },
        methods: {
            showColorPicker () {
                this.$refs.color.click();
            },
            brightnessByColor (color) {
                const match = color.substr(1).match(color.length === 7 ? /(\S{2})/g : /(\S)/g);

                if (match) {
                    const r = parseInt(match[0], 16);
                    const g = parseInt(match[1], 16);
                    const b = parseInt(match[2], 16);

                    return (
                        (r * 299) +
                        (g * 587) +
                        (b * 114)
                    ) / 1000;
                }

                return null;
            },
        },
    });
</script>

<style scoped>
    input[type="color"] {
        visibility: hidden;
        display: inline;
    }
</style>
