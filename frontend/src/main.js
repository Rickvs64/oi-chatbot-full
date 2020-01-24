import Vue from 'vue'
import Router from 'vue-router'
import App from './App.vue'
import BootstrapVue from 'bootstrap-vue'
import axios from 'axios'
import VueAxios from 'vue-axios'


import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'
import router from './router'

require('./assets/css/custom.css')
require('./assets/css/spinner.css')

Vue.use(BootstrapVue)
Vue.use(Router)
Vue.use(VueAxios, axios)
Vue.prototype.$axios = axios

Vue.config.productionTip = false

new Vue({
    router,
    render: h => h(App)
}).$mount('#app')