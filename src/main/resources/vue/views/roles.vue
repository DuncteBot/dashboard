<template id="roles">
    <div class="container">
        <h1>Roles for {{ $javalin.state.guildName }}</h1>
        <table class="striped centered">
            <thead>
                <tr>
                    <th>Id</th>
                    <th>name</th>
                    <th>Members with this role</th>
                </tr>
            </thead>
            <tbody>
                <tr :style="{
                    backgroundColor: parseColor('536870911'/*$role.getColorRaw()*/)
                }">
                    <td>$role.getIdLong()</td>
                    <td>$role.getName()</td>
                    <td>$role.getMemberCount() Members</td>
                </tr>
            </tbody>
        </table>
    </div>
</template>

<script>
    const defaultColor = '536870911';

    Vue.component('roles', { // the component name is what we need to enter in VueComponent("...")
        template: '#roles',
        methods: {
            parseColor (col) {
                if (col === defaultColor) {
                    // default discord role color
                    return 'rgb(153, 170, 181)';
                }

                const b = col & 0xFF;
                const g = (col & 0xFF00) >>> 8;
                const r = (col & 0xFF0000) >>> 16;

                return 'rgb(' + [r, g, b].join(',') + ')';
            },
        },
    });
</script>

<style>
    table {
        margin-bottom: 20px;
    }
</style>
