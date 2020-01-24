<template>
  <div class="input">

    <div class="row">
      <div class="col-md-8" id="inputcontainer">
        <label for="textinput"><h1>Input:</h1></label>
        <input class="form-control" id="textinput" type="text" v-model='currentMessage' @keyup.enter='submitMessage()'>
      </div>
    </div>

  </div>
</template>

<script>
export default {
  name: "Input",
  data() {
    return {
      currentMessage: ''
    };
  },
  mounted() {
    
  },
  methods: {
    submitMessage() {
      // Check for invalid input text and exit function accordingly.
      if (!this.currentMessage) {
        return;
      }
      this.sendToFrontEnd(this.currentMessage);
      
      // Clear variable and input field.
      this.currentMessage = '';
    },
    sendToFrontEnd(message) {
      localStorage.setItem("input", message);
    }
  }
}
</script>