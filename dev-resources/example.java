class Example {
    static int i = 0;
    public static void doSomething(){
        if(i % 2 == 0){
            System.out.println("Even: " + i);
        } else {
            System.out.println("Odd:  " + i);
        }
        i++;
    }
    public static void main(String[] args) throws InterruptedException {
        int i = 0;
        System.out.println("Hello!");
        while(true){
            doSomething();
            Thread.sleep(1000);
        }
    }
}
