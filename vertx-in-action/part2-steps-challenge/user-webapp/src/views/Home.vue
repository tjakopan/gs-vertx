<template>
  <div>
    <div class="alert alert-danger" role="alert" v-if="alertMessage.length > 0">
      {{ alertMessage }}
    </div>
    <div class="float-right">
      <button v-on:click="logout" class="btn btn-outline-danger" type="button">logout</button>
    </div>
    <div>
      <h3>Welcome!</h3>
      <ul>
        <li>Username: {{ username }}</li>
        <li>Device identifier: {{ deviceId }}</li>
        <li>
          You have done <span class="badge badge-success">{{ totalSteps }}</span> steps in total,
          <span class="badge badge-success">{{ stepsForMonth }}</span> this month, and
          <span class="badge badge-success">{{ stepsForToday }}</span> today.
        </li>
      </ul>
    </div>
    <div class="mt-5">
      <h5>Update your details</h5>
      <form v-on:submit="sendUpdate">
        <div class="form-group">
          <label for="email">Email</label>
          <input type="email" class="form-control" id="email" placeholder="foo@mail.me" v-model="email">
        </div>
        <div class="form-group">
          <label for="city">City</label>
          <input type="city" class="form-control" id="city" placeholder="Lyon" v-model="city">
        </div>
        <div class="form-check">
          <input class="form-check-input" type="checkbox" id="makePublic" v-model="makePublic">
          <label class="form-check-label" for="makePublic">
            I want to appear in public rankings
          </label>
        </div>
        <div class="form-group">
          <button type="submit" class="btn btn-outline-primary">Submit</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import dataStore from "@/dataStore";
import axios from "axios";

export default {
  data() {
    return {
      username: 'n/a',
      city: 'n/a',
      email: 'n/a',
      deviceId: 'n/a',
      makePublic: false,
      totalSteps: 0,
      stepsForMonth: 0,
      stepsForToday: 0,
      alertMessage: ''
    }
  },
  mounted() {
    if (!dataStore.hasToken()) {
      this.$router.push({name: 'login'})
      return
    }
    this.refreshData()
    this.username = dataStore.username()
  },
  methods: {
    logout() {
      dataStore.reset()
      this.$router.push({name: 'login'})
    },
    refreshData() {
      axios.get(`http://localhost:4000/api/v1/${dataStore.username()}`, {
        headers: {
          'Authorization': `Bearer ${dataStore.token()}`
        }
      })
          .then(response => {
            dataStore.setCity(response.data.city)
            dataStore.setDeviceId(response.data.deviceId)
            dataStore.setEmail(response.data.email)
            dataStore.setMakePublic(response.data.makePublic)
            this.refreshFromDataStore()
          })
          .catch(err => this.alertMessage = err.message)

      const today = new Date()

      axios.get(`http://localhost:4000/api/v1/${dataStore.username()}/total`, {
        headers: {
          'Authorization:': `Bearer ${dataStore.token()}`
        }
      })
          .then(response => this.totalSteps = response.data.count)
          .catch(err => {
            if (err.response.status === 404) {
              this.totalSteps = 0
            } else {
              this.alertMessage = err.message
            }
          })

      axios.get(`http://localhost:4000/api/v1/${dataStore.username()}/${today.getFullYear()}/${today.getUTCMonth() + 1}/${today.getDate()}`, {
        headers: {
          'Authorization:': `Bearer ${dataStore.token()}`
        }
      })
          .then(response => this.stepsForToday = response.data.count)
          .catch(err => {
            if (err.response.status === 404) {
              this.stepsForToday = 0
            } else {
              this.alertMessage = err.message
            }
          })
    },
    refreshFromDataStore() {
      this.city = dataStore.city()
      this.deviceId = dataStore.deviceId()
      this.email = dataStore.email()
      this.makePublic = dataStore.makePublic()
    },
    sendUpdate() {
      const data = {
        city: this.city,
        email: this.email,
        makePublic: this.makePublic
      }
      const config = {
        headers: {
          'Authorization:': `Bearer ${dataStore.token()}`
        }
      }
      axios.put(`http://localhost:4000/api/v1/${dataStore.username()}`, data, config)
          .then(() => this.refreshData())
          .catch(err => {
            this.alertMessage = err.message
            this.refreshFromDataStore()
          })
    }
  }
}
</script>
