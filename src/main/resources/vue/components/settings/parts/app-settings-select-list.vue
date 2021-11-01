<template id="settings-select-list">
    <div class="input-field">
        <select v-model="dataValue">
            <option value="0" selected disabled>Select a {{ name }}</option>
            <option v-for="item in options"
                    :key="item.id"
                    :value="item.id">{{ prefix }}{{ item.name }}</option>
            <option v-if="!hideDisabled" value="0">Disable</option>
        </select>
        <label>{{ label }}</label>
    </div>
</template>

<script>
    Vue.component('select-list', {
        template: '#settings-select-list',
        props: {
            label: {
                type: String,
                required: true,
            },
            name: {
                type: String,
                required: true,
            },
            prefix: {
                type: String,
                required: true,
            },
            options: {
                type: Array,
                required: true,
            },
            value: {
                type: String,
                required: true,
            },
            hideDisabled: Boolean,
        },
        data () {
            return {
                dataValue: this.value,
            };
        },
        watch: {
            dataValue () {
                this.$emit('input', this.dataValue);
            },
        },
        computed: {
            compId () {
                return this.name + this.label.replaceAll(' ', '');
            },
        },
    })
</script>
