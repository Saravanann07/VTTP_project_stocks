package vttp2022.project.Stock.controllers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import vttp2022.project.Stock.exceptions.TransactionException;
import vttp2022.project.Stock.exceptions.UserException;
import vttp2022.project.Stock.models.Transaction;
import vttp2022.project.Stock.models.User;
import vttp2022.project.Stock.repositories.UserRepository;
import vttp2022.project.Stock.services.StockService;
import vttp2022.project.Stock.services.TransactionService;
import vttp2022.project.Stock.services.UserService;

@Controller
@RequestMapping("")
public class UserController {

    @Autowired 
    private UserService userSvc;

    @Autowired
    private TransactionService transactionSvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockService stockSvc;

    @GetMapping(path="")
    public String getLoginPage(Model model){
        return "login_page";
    }

    @GetMapping(path="/register")
    public String getRegisterPage(Model model){
        return "register";
    }

    @GetMapping(path="/logout")
    public ModelAndView getLogout(HttpSession sess) {
        sess.invalidate();
        ModelAndView mvc = new ModelAndView();
        mvc.addObject("messageLogout", "You have successfully logged out");
        mvc.setViewName("login_page");

        return mvc;
    }

    @PostMapping(path = "/createUser")
    public ModelAndView createUser(@RequestBody MultiValueMap<String, String> form) {

        ModelAndView mvc = new ModelAndView();

        String username = form.getFirst("username");
        String password = form.getFirst("password");

        try {
            userSvc.createUser(username, password);
            mvc.addObject("messageUser", "%s has been successfully registered".formatted(username));

        } catch (UserException ex) {
            mvc.addObject("messageUser", "Error: %s".formatted(ex.getReason()));
            mvc.setStatus(HttpStatus.BAD_REQUEST);
            ex.printStackTrace();
        }

        mvc.setViewName("register");
        return mvc;
    }

    @PostMapping(path="/authenticate")
    public ModelAndView postLogin(
        @RequestBody MultiValueMap<String, String> payload, HttpSession sess) {
            // name in login_page should match multivaluemap (getFirst)
            String username = payload.getFirst("username");
            String password = payload.getFirst("password");
            System.out.println(">>>>>>>>" + payload);
            User user = userRepository.getUser(username, password);

            ModelAndView mvc = new ModelAndView();
            
            // not successful
            if (!userSvc.authenticate(username, password)) {
                mvc.setStatus(HttpStatus.UNAUTHORIZED);
                mvc.addObject("messageLoginError", "Error! Invalid username or password");
                mvc.setViewName("login_page");
                // return mvc;

             //successful   
            } else{     
            mvc.addObject("username", username);

            Optional<List<Transaction>> optTransaction = transactionSvc.getDateTransactions(user.getUserId());

            List<Transaction> transactionList = optTransaction.get();
            for (Transaction trans : transactionList) {

                 
                //Gets market value of stocks everytime user refreshes page
                Double marketPrice = stockSvc.getQuote(trans.getSymbol());
                Double marketValue = marketPrice*trans.getQuantity();
                BigDecimal bd = new BigDecimal(Double.toString(marketValue));
                bd = bd.setScale(2, RoundingMode.HALF_DOWN);
                trans.setStockStatus(bd);

                System.out.println(">>>>>" + marketPrice);
                System.out.println(">>>>>>" + trans.getStockStatus());
            }

            
            mvc.addObject("transactionList", transactionList);
            mvc.setViewName("HomePage");
            mvc.setStatus(HttpStatus.ACCEPTED);
            mvc.setViewName("Homepage");
            
            sess.setAttribute("username", username);
            sess.setAttribute("password", password);
            }
                
            return mvc;

        }

    @PostMapping(path="/addTransaction")
    public ModelAndView addStockPurchase(@RequestBody MultiValueMap<String, String> form, HttpSession sess) {
        
            String username = (String) sess.getAttribute("username");
            String password = (String) sess.getAttribute("password");
            


        ModelAndView mvc = new ModelAndView();
        String dateStr = form.getFirst("purchaseDate");

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date purchaseDate;
        try {
            purchaseDate = format.parse(dateStr);
            System.out.println(">>>>>>" + purchaseDate);
        } catch (ParseException e) {
            purchaseDate = null;
            e.printStackTrace();
        }

        String symbol = form.getFirst("symbol");
        String companyName = form.getFirst("companyName");
        Integer quantity = Integer.parseInt(form.getFirst("quantity"));
        Double stockPrice = Double.parseDouble(form.getFirst("stockPrice"));
        Double totalPrice = Double.parseDouble(form.getFirst("totalPrice"));
        User user = userRepository.getUser(username, password);
        
    
        try {
            transactionSvc.addTransaction(user.getUserId(), purchaseDate, symbol, companyName, quantity, stockPrice, totalPrice);
            mvc.addObject("transactionUser", "%s has been successfully added to your stock purchases".formatted(symbol));

        } catch (TransactionException ex) {
            mvc.addObject("transactionMessage", "error: %s".formatted(ex.getReason()));
            mvc.setStatus(HttpStatus.BAD_REQUEST);
            ex.printStackTrace();
            
        }
        System.out.println(">>>>>>>>>errrrorrrrr");

        Optional<List<Transaction>> optTransaction = transactionSvc.getDateTransactions(user.getUserId());
        List<Transaction> transactionList = optTransaction.get();
        
        for (Transaction trans : transactionList) {
            
            Double marketPrice = stockSvc.getQuote(trans.getSymbol());
            Double marketValue = marketPrice*trans.getQuantity();
            BigDecimal bd = new BigDecimal(Double.toString(marketValue));
            bd = bd.setScale(2, RoundingMode.HALF_DOWN);
            trans.setStockStatus(bd);
            System.out.println(">>>>>" + marketPrice);
            System.out.println(">>>>>>" + trans.getStockStatus());
        }
        mvc.addObject("username", username);
        mvc.addObject("transactionList", transactionList);
        mvc.setViewName("Homepage");
        return mvc;
    }

    // Group all purchases made by user from a specific company. Identified by symbol/ticker
    @GetMapping(path= "/company") 
        public ModelAndView getUserCompanyPurchase(@RequestParam("symbol") String symbol, HttpSession sess) {

            String username = (String) sess.getAttribute("username");
            String password = (String) sess.getAttribute("password");

            ModelAndView mvc = new ModelAndView();

            User user = userRepository.getUser(username, password);

            Optional<List<Transaction>> optCompany = transactionSvc.getCompanyTransactions(symbol, user.getUserId());
            List<Transaction> allPurchasesList = optCompany.get();

            for (Transaction trans : allPurchasesList) {
            
                Double marketPrice = stockSvc.getQuote(trans.getSymbol());
                Double marketValue = marketPrice*trans.getQuantity();
                BigDecimal bd = new BigDecimal(Double.toString(marketValue));
                bd = bd.setScale(2, RoundingMode.HALF_DOWN);
                trans.setStockStatus(bd);
                System.out.println(">>>>>" + marketPrice);
                System.out.println(">>>>>>" + trans.getStockStatus());
            }

            mvc.addObject("allPurchasesList", allPurchasesList);
            mvc.setViewName("company");

            return mvc;
        }

        @GetMapping(path="/homepage")
            public ModelAndView getHomePage(HttpSession sess) {

                String username = (String) sess.getAttribute("username");
                String password = (String) sess.getAttribute("password");

                ModelAndView mvc = new ModelAndView();

                User user = userRepository.getUser(username, password);

                Optional<List<Transaction>> optTransaction = transactionSvc.getDateTransactions(user.getUserId());

                List<Transaction> transactionList = optTransaction.get();
                
                for (Transaction trans : transactionList) {
                    
                    Double marketPrice = stockSvc.getQuote(trans.getSymbol());
                    Double marketValue = marketPrice*trans.getQuantity();
                    BigDecimal bd = new BigDecimal(Double.toString(marketValue));
                    bd = bd.setScale(2, RoundingMode.HALF_DOWN);
                    trans.setStockStatus(bd);
                    System.out.println(">>>>>" + marketPrice);
                    System.out.println(">>>>>>" + trans.getStockStatus());
                }
                mvc.addObject("username", username);
                mvc.addObject("transactionList", transactionList);
                mvc.setViewName("Homepage");
                return mvc;

            }

            // @PostMapping(path="/deleteTransaction")
            // public ModelAndView deleteTransaction(Double totalPrice, String symbol, HttpSession sess) {
                
            //         String username = (String) sess.getAttribute("username");
            //         String password = (String) sess.getAttribute("password");

                    
                    
        
        
            //     ModelAndView mvc = new ModelAndView();
            //     User user = userRepository.getUser(username, password);
                
            //     transactionSvc.deleteTransaction(totalPrice);
            //     mvc.addObject("deleteMessage", "Transaction from %s has been deleted".formatted(symbol));
                
            
            //     // try {
            //     //     transactionSvc.addTransaction(user.getUserId(), purchaseDate, symbol, companyName, quantity, stockPrice, totalPrice);
            //     //     mvc.addObject("transactionUser", "%s has been successfully added to your stock purchases".formatted(symbol));
        
            //     // } catch (TransactionException ex) {
            //     //     mvc.addObject("transactionMessage", "error: %s".formatted(ex.getReason()));
            //     //     mvc.setStatus(HttpStatus.BAD_REQUEST);
            //     //     ex.printStackTrace();
                    
            //     // }
            //     System.out.println(">>>>>>>>>errrrorrrrr");
        
            //     Optional<List<Transaction>> optTransaction = transactionSvc.getDateTransactions(user.getUserId());
            //     List<Transaction> transactionList = optTransaction.get();
                
            //     for (Transaction trans : transactionList) {
                    
            //         Double marketPrice = stockSvc.getQuote(trans.getSymbol());
            //         Double marketValue = marketPrice*trans.getQuantity();
            //         BigDecimal bd = new BigDecimal(Double.toString(marketValue));
            //         bd = bd.setScale(2, RoundingMode.HALF_DOWN);
            //         trans.setStockStatus(bd);
            //         System.out.println(">>>>>" + marketPrice);
            //         System.out.println(">>>>>>" + trans.getStockStatus());
            //     }
            //     mvc.addObject("username", username);
            //     mvc.addObject("transactionList", transactionList);
            //     mvc.setViewName("Homepage");
            //     return mvc;
            // }


    
}